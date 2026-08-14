import csv
import requests
import re
import json

SUPABASE_URL = "https://brgwlyixvgdvzahmsusf.supabase.co/rest/v1/track_metadata"
SUPABASE_KEY = "sb_publishable_4qGbvRV8ArCt3OkFe4mcCQ_r9DpCKM1"
CSV_PATH = r"d:\Descargas (S)\Milla\Lyrics\Master_Discografias_Metricas_Artistas_1_al_100 - Untitled.csv"

def generate_track_id(artist, title):
    clean_artist = re.sub(r'[^a-zA-Z0-9]', '', artist).lower()
    clean_title = re.sub(r'[^a-zA-Z0-9]', '', title).lower()
    return f"{clean_artist}_{clean_title}"

def main():
    print(f"Abriendo archivo: {CSV_PATH}")
    rows = []
    
    with open(CSV_PATH, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                artist = row.get("Artista", "").strip()
                title = row.get("Título Canción", "").strip()
                if not artist or not title:
                    continue
                    
                bpm_str = row.get("BPM Base", "0")
                bpm = float(bpm_str) if bpm_str.replace('.','',1).isdigit() else 0.0
                
                ht_str = row.get("Half-Time", "0")
                ht = float(ht_str) if ht_str.replace('.','',1).isdigit() else 0.0
                
                dt_str = row.get("Double-Time", "0")
                dt = float(dt_str) if dt_str.replace('.','',1).isdigit() else 0.0
                
                compas_str = row.get("Compás", "")
                time_sig = 4
                if compas_str and "/" in compas_str:
                    time_sig = int(compas_str.split("/")[0])
                
                key = row.get("Tonalidad (Key)", "")
                mode = "major" if "Mayor" in key else "minor"
                
                data = {
                    "track_id": generate_track_id(artist, title),
                    "title": title,
                    "artist": artist,
                    "bpm": bpm,
                    "half_time_bpm": ht,
                    "double_time_bpm": dt,
                    "musical_key": key,
                    "mode": mode,
                    "time_signature": time_sig,
                    "cue_out_ms": 0,
                    "replay_gain": 0.0
                }
                rows.append(data)
            except Exception as e:
                print(f"Error parseando fila: {e}")

    print(f"Total a insertar: {len(rows)} canciones.")
    
    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates"
    }

    # Batch insert in chunks of 500
    batch_size = 500
    for i in range(0, len(rows), batch_size):
        batch = rows[i:i+batch_size]
        res = requests.post(SUPABASE_URL, json=batch, headers=headers)
        if res.status_code in [200, 201]:
            print(f"✅ Lote {i} - {i+len(batch)} insertado con éxito.")
        else:
            print(f"❌ Error insertando lote: {res.status_code} - {res.text}")

if __name__ == "__main__":
    main()
