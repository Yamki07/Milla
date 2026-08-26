import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
  const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
  const geminiApiKey = Deno.env.get("GEMINI_API_KEY") ?? ""

  if (!geminiApiKey) {
    return new Response(JSON.stringify({ error: "Missing GEMINI_API_KEY" }), { status: 500, headers: corsHeaders })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  try {
    // 1. Obtener hasta 10 canciones pendientes
    // Seleccionamos aquellas que tengan bpm en nulo o géneros nulos.
    const { data: pendingTracks, error: fetchError } = await supabase
      .from("track_metadata")
      .select("track_id, title, artist, bpm, genres, moods")
      .or("bpm.is.null,genres.is.null")
      .limit(10)

    if (fetchError) throw fetchError

    if (!pendingTracks || pendingTracks.length === 0) {
      return new Response(
        JSON.stringify({ message: "No hay pistas pendientes por procesar." }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const results = []

    // 2. Procesar secuencialmente en segundo plano usando Gemini 2.5 Flash
    for (const track of pendingTracks) {
      try {
        const payload = {
          system_instruction: {
            parts: { 
              text: "Eres un investigador musical avanzado y curador de DJs. Usa Google Search para buscar metadatos exactos de la canción solicitada. Devuelve la información estrictamente según el esquema JSON solicitado." 
            }
          },
          contents: [
            {
              role: "user",
              parts: [
                { text: `Encuentra metadatos musicales para la canción: '${track.title}' del artista '${track.artist}'. Busca BPM, Camelot Key, géneros, emociones (moods), energía (High/Medium/Low), bailabilidad (High/Medium/Low), compás (time_signature), modo (Major/Minor), y también provee una traducción de la letra al español si la encuentras.` }
              ]
            }
          ],
          tools: [
            { googleSearch: {} }
          ],
          generationConfig: {
            responseMimeType: "application/json",
            responseSchema: {
              type: "OBJECT",
              properties: {
                bpm: { type: "NUMBER", description: "BPM de la canción (ej. 128.0)" },
                musical_key: { type: "STRING", description: "Camelot Key (ej. 8B) o nota (ej. C Major)" },
                genres: { type: "ARRAY", items: { type: "STRING" }, description: "Lista de géneros musicales" },
                moods: { type: "ARRAY", items: { type: "STRING" }, description: "Lista de emociones o moods" },
                energy: { type: "STRING", description: "High, Medium, o Low" },
                danceability: { type: "STRING", description: "High, Medium, o Low" },
                time_signature: { type: "INTEGER", description: "Ejemplo: 4" },
                mode: { type: "STRING", description: "Major o Minor" },
                synced_lyrics_translated: { type: "STRING", description: "Letra traducida en texto plano, o vacío si no se encuentra" }
              },
              required: ["bpm", "genres", "moods"]
            }
          }
        };

        const geminiResponse = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiApiKey}`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          }
        );

        let parsed: any = null;

        if (geminiResponse.ok) {
          const geminiData = await geminiResponse.json();
          const rawText = geminiData?.candidates?.[0]?.content?.parts?.[0]?.text;
          
          if (rawText) {
            try {
              parsed = JSON.parse(rawText);
            } catch (parseErr) {
              console.error(`Error parseando JSON nativo de Gemini para ${track.track_id}:`, parseErr);
            }
          }
        } else {
            console.error(`Error de Gemini API para ${track.track_id}:`, await geminiResponse.text());
        }

        // CORTAFUEGOS ANTI-BUCLES:
        // Si parsed es nulo (por error de red, parsing fallido o canción no encontrada),
        // guardamos valores seguros por defecto para que la base de datos DEJE DE SER NULL.
        // Esto previene que la misma canción sea procesada infinitamente.
        if (!parsed) {
            parsed = {
                bpm: track.bpm || 0, // Conserva el que tiene o pone 0
                musical_key: "",
                genres: [],
                moods: [],
                energy: "",
                danceability: "",
                time_signature: 4,
                mode: "",
                synced_lyrics_translated: ""
            };
        }

        // Calcular halftime y doubletime
        const bpm_val = typeof parsed.bpm === "number" ? parsed.bpm : null;
        const half_bpm = bpm_val && bpm_val > 0 ? bpm_val / 2 : null;
        const double_bpm = bpm_val && bpm_val > 0 ? bpm_val * 2 : null;

        // 3. Sobreescribir directamente en la tabla de Supabase
        const { error: updateError } = await supabase
          .from("track_metadata")
          .update({
            bpm: bpm_val,
            half_time_bpm: half_bpm,
            double_time_bpm: double_bpm,
            musical_key: parsed.musical_key || null,
            genres: Array.isArray(parsed.genres) ? parsed.genres : [], // Forzamos JSONB
            moods: Array.isArray(parsed.moods) ? parsed.moods : [], // Forzamos JSONB
            energy: parsed.energy || null,
            danceability: parsed.danceability || null,
            time_signature: parsed.time_signature || null,
            mode: parsed.mode || null,
            synced_lyrics_translated: parsed.synced_lyrics_translated || null,
            updated_at: new Date().toISOString()
          })
          .eq("track_id", track.track_id)

        if (!updateError) {
          results.push({ track_id: track.track_id, status: parsed.bpm > 0 ? "enriched" : "failed_but_marked_as_processed" })
        } else {
          console.error(`Error de Supabase update en ${track.track_id}:`, updateError)
        }

      } catch (err) {
        console.error(`Error crítico procesando track ${track.track_id}:`, err)
      }
    }

    return new Response(
      JSON.stringify({ success: true, processed: results }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: (error as Error).message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
