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
  const youApiKey = Deno.env.get("YOUCOM_API_KEY") ?? ""

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  try {
    // 1. Obtener hasta 10 canciones que aún no tengan BPM o letras cargadas
    const { data: pendingTracks, error: fetchError } = await supabase
      .from("track_metadata")
      .select("track_id, title, artist")
      .or("bpm.is.null,genres.is.null,synced_lyrics.is.null")
      .limit(10)

    if (fetchError) throw fetchError

    if (!pendingTracks || pendingTracks.length === 0) {
      return new Response(
        JSON.stringify({ message: "No hay pistas pendientes por procesar." }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const results = []

    // 2. Procesar secuencialmente en segundo plano
    for (const track of pendingTracks) {
      const systemPrompt = `Actúa como investigador musical avanzado y curador de DJs.
Busca en la web profunda y bases de datos los metadatos de la canción '${track.title}' del artista '${track.artist}'.
Necesito: BPM, Camelot Key, géneros, emociones (moods), cue_out_ms, energía (High/Medium/Low), bailabilidad (High/Medium/Low), compás (time_signature, ej. 4), modo (Major/Minor), y canciones recomendadas que mezclen perfecto. Y MUY IMPORTANTE: la letra sincronizada por sílabas en formato JSON.
Devuelve EXCLUSIVAMENTE un JSON válido sin markdown:
{
  "bpm": 128.0,
  "musical_key": "8B",
  "genres": ["Reggaeton", "Urbano"],
  "moods": ["Energetic", "Party"],
  "cue_in_ms": 0,
  "cue_out_ms": 180000,
  "energy": "High",
  "danceability": "High",
  "time_signature": 4,
  "mode": "Major",
  "perfect_match_songs": ["Cancion 1", "Cancion 2"],
  "synced_lyrics": [{"time": 0, "text": "Intro", "durationMs": 2000}]
}`

      try {
        const youResponse = await fetch("https://api.you.com/v1/research", {
          method: "POST",
          headers: {
            "X-API-Key": youApiKey,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            input: `${systemPrompt}\n\nTask: Busca BPM, Camelot Key, géneros, energía, bailabilidad, time_signature y canciones compatibles de ${track.title} - ${track.artist}`,
            research_effort: "standard"
          }),
        })

        if (!youResponse.ok) continue

        const youData = await youResponse.json()
        const rawAnswer = youData.answer || youData.response || ""
        const jsonMatch = rawAnswer.match(/\{[\s\S]*\}/)

        if (jsonMatch) {
          const parsed = JSON.parse(jsonMatch[0])

          // Calcular halftime y doubletime
          const bpm_val = typeof parsed.bpm === "number" ? parsed.bpm : null;
          const half_bpm = bpm_val ? bpm_val / 2 : null;
          const double_bpm = bpm_val ? bpm_val * 2 : null;

          // 3. Sobreescribir directamente en la tabla de Supabase
          const { error: updateError } = await supabase
            .from("track_metadata")
            .update({
              bpm: bpm_val,
              half_time_bpm: half_bpm,
              double_time_bpm: double_bpm,
              musical_key: parsed.musical_key || null,
              genres: parsed.genres || null,
              moods: parsed.moods || null,
              energy: parsed.energy || null,
              danceability: parsed.danceability || null,
              time_signature: parsed.time_signature || null,
              mode: parsed.mode || null,
              cue_in_ms: parsed.cue_in_ms || 0,
              cue_out_ms: parsed.cue_out_ms || null,
              synced_lyrics: parsed.synced_lyrics || [],
              updated_at: new Date().toISOString()
            })
            .eq("track_id", track.track_id)

          if (!updateError) {
            results.push({ track_id: track.track_id, status: "updated" })
          }
        }
      } catch (err) {
        console.error(`Error procesando track ${track.track_id}:`, err)
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
