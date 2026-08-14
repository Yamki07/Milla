import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  // Manejo de peticiones pre-flight CORS
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { lyrics, targetLanguage } = await req.json();

    if (!lyrics || !targetLanguage) {
      return new Response(
        JSON.stringify({ success: false, error: "Missing 'lyrics' or 'targetLanguage'" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
      );
    }

    // Extracción segura de la API Key desde el entorno de Deno
    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) {
      throw new Error("GEMINI_API_KEY environment variable is not set.");
    }

    // Configuración de la instrucción del sistema (Crítico para preservar tiempos)
    const systemInstruction = "Eres un sistema especializado en procesamiento de metadatos musicales y lingüísticos para la aplicación Milla. Tu única tarea es recibir texto de letras musicales sincronizadas (en formato LRC o plano), traducir el contenido al idioma solicitado o limpiar errores ortográficos, y respetar de forma absoluta cada marca de tiempo (ej. [00:12.34]). Jamás debes alterar, mover o eliminar los corchetes de tiempo ni las etiquetas de sincronización. Devuelve únicamente el texto procesado.";

    // Construcción del payload para Gemini
    const payload = {
      system_instruction: {
        parts: { text: systemInstruction }
      },
      contents: [
        {
          role: "user",
          parts: [
            { text: `Traduce esta letra al idioma ${targetLanguage}, manteniendo intactas todas las marcas de tiempo:\n\n${lyrics}` }
          ]
        }
      ]
    };

    // Llamada oficial a la API de Google Gemini (modelo recomendado para texto rápido)
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Gemini API Error: ${response.status} ${errorText}`);
    }

    const data = await response.json();
    
    // Extracción segura de la respuesta de Gemini
    const translatedLyrics = data?.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!translatedLyrics) {
      throw new Error("No valid response from Gemini");
    }

    // Devolvemos el JSON estructurado al cliente
    return new Response(
      JSON.stringify({ success: true, translatedLyrics }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
    );

  } catch (error) {
    console.error("Error processing request:", error);
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});
