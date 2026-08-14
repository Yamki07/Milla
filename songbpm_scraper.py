import requests
import re
import time
import csv
from bs4 import BeautifulSoup

# --- CONFIGURACIÓN SUPABASE ---
SUPABASE_URL = "https://brgwlyixvgdvzahmsusf.supabase.co/rest/v1/track_metadata"
SUPABASE_KEY = "sb_publishable_4qGbvRV8ArCt3OkFe4mcCQ_r9DpCKM1"

# --- ARTISTAS A SCRAPEAR (LOTE 1) ---
ARTISTS = [
    "Bad Bunny",
    "Daddy Yankee",
    "Karol G",
    "J Balvin",
    "Rauw Alejandro"
]

def generate_track_id(artist, title):
    # Simplificación del generador de ID que tienes en Kotlin
    # Remueve caracteres especiales y espacios
    clean_artist = re.sub(r'[^a-zA-Z0-9]', '', artist).lower()
    clean_title = re.sub(r'[^a-zA-Z0-9]', '', title).lower()
    return f"{clean_artist}_{clean_title}"

def search_artist_songs(artist):
    print(f"\nBuscando canciones de: {artist}")
    # SongBPM no tiene un endpoint de "todos los álbumes", así que buscamos al artista.
    search_url = f"https://songbpm.com/searches?q={artist.replace(' ', '+')}"
    headers = {'User-Agent': 'Mozilla/5.0'}
    try:
        response = requests.get(search_url, headers=headers)
        if response.status_code != 200:
            print(f"Error {response.status_code} al buscar {artist}")
            return []
            
        soup = BeautifulSoup(response.text, 'html.parser')
        links = soup.find_all('a', href=True)
        song_urls = []
        for a in links:
            href = a['href']
            # URLs de canciones suelen ser formato /@artista/cancion-id
            if href.startswith('/@') and artist.lower().split()[0] in href.lower():
                full_url = f"https://songbpm.com{href}"
                if full_url not in song_urls:
                    song_urls.append(full_url)
        return song_urls
    except Exception as e:
        print(f"Error buscando {artist}: {e}")
        return []

def extract_metadata(url):
    headers = {'User-Agent': 'Mozilla/5.0'}
    try:
        response = requests.get(url, headers=headers)
        if response.status_code != 200:
            return None
            
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # Extraer Título y Artista del h1 (ej: "Tití Me Preguntó by Bad Bunny")
        title = ""
        artist = ""
        h1 = soup.find('h1')
        if h1:
            parts = h1.text.split(' by ')
            if len(parts) == 2:
                title = parts[0].strip()
                artist = parts[1].strip()
        
        # Buscar el párrafo descriptivo
        paragraph_text = ""
        paragraphs = soup.find_all('p')
        for p in paragraphs:
            if "is a" in p.text and "song by" in p.text and "tempo of" in p.text:
                paragraph_text = p.text
                break
                
        if not paragraph_text:
            return None
            
        # Parseo con Regex (Idéntico a tu SongBpmScraper.kt)
        bpm = re.search(r"tempo of ([\d.]+) BPM", paragraph_text)
        bpm = float(bpm.group(1)) if bpm else 0.0
        
        mood = re.search(r"is a (.*?) song by", paragraph_text)
        mood = mood.group(1) if mood else ""
        
        half_time = re.search(r"half-time at ([\d.]+) BPM", paragraph_text)
        half_time = float(half_time.group(1)) if half_time else 0.0
        
        double_time = re.search(r"double-time at ([\d.]+) BPM", paragraph_text)
        double_time = float(double_time.group(1)) if double_time else 0.0
        
        key_match = re.search(r"with a (.*?) key", paragraph_text)
        key = key_match.group(1) if key_match else ""
        
        mode = re.search(r"and a (major|minor) mode", paragraph_text)
        mode = mode.group(1) if mode else ""
        
        energy = re.search(r"It has (.*?) and is", paragraph_text)
        energy = energy.group(1) if energy else ""
        
        danceability = re.search(r"and is (.*?) with a time", paragraph_text)
        danceability = danceability.group(1) if danceability else ""
        
        time_sig = re.search(r"time signature of (\d+) beats", paragraph_text)
        time_sig = int(time_sig.group(1)) if time_sig else 0
        
        return {
            "title": title,
            "artist": artist,
            "bpm": bpm,
            "mood": mood,
            "half_time_bpm": half_time,
            "double_time_bpm": double_time,
            "musical_key": key,
            "mode": mode,
            "energy": energy,
            "danceability": danceability,
            "time_signature": time_sig
        }
    except Exception as e:
        print(f"Error extrayendo de {url}: {e}")
        return None

def upload_to_supabase(metadata):
    track_id = generate_track_id(metadata['artist'], metadata['title'])
    
    payload = {
        "track_id": track_id,
        "title": metadata['title'],
        "artist": metadata['artist'],
        "bpm": metadata['bpm'],
        "musical_key": metadata['musical_key'],
        "mood": metadata['mood'],
        "half_time_bpm": metadata['half_time_bpm'],
        "double_time_bpm": metadata['double_time_bpm'],
        "mode": metadata['mode'],
        "energy": metadata['energy'],
        "danceability": metadata['danceability'],
        "time_signature": metadata['time_signature'],
        "cue_out_ms": 0,
        "replay_gain": 0.0
    }
    
    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates"
    }
    
    response = requests.post(SUPABASE_URL, json=payload, headers=headers)
    if response.status_code in [200, 201]:
        print(f"✅ Subido: {metadata['title']} - {metadata['artist']}")
    else:
        print(f"❌ Error subiendo {metadata['title']}: {response.text}")

def main():
    print("Iniciando Escarabajo Python...")
    
    # Crear archivo CSV para respaldo
    csv_file = open("canciones_extraidas.csv", "w", newline="", encoding="utf-8")
    writer = csv.writer(csv_file)
    writer.writerow(["Artista", "Título Canción", "BPM Base", "Half-Time", "Double-Time", "Compás", "Tonalidad (Key)", "Modo", "Energía", "Bailabilidad", "Mood"])
    
    total_canciones = 0
    
    for artist in ARTISTS:
        urls = search_artist_songs(artist)
        print(f"Se encontraron {len(urls)} enlaces posibles para {artist}")
        
        for url in urls:
            meta = extract_metadata(url)
            if meta and meta['title']:
                upload_to_supabase(meta)
                
                # Escribir en CSV
                writer.writerow([
                    meta['artist'], meta['title'], meta['bpm'], 
                    meta['half_time_bpm'], meta['double_time_bpm'], 
                    meta['time_signature'], meta['musical_key'], 
                    meta['mode'], meta['energy'], meta['danceability'], meta['mood']
                ])
                total_canciones += 1
                
            time.sleep(1) # Delay para no bloquear la IP
            
    csv_file.close()
    print(f"\n¡Completado! Se extrajeron y subieron {total_canciones} canciones.")

if __name__ == "__main__":
    main()
