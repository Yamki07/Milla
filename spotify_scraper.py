import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
import csv
import re

# ==========================================
# 🔴 ATENCIÓN: Pega tus credenciales aquí 🔴
# ==========================================
SPOTIPY_CLIENT_ID = "57706a980f2043dc9e7e5c4c60e15924"
SPOTIPY_CLIENT_SECRET = "b1b46434500f4b93888df96784663679"

def get_camelot_key(key, mode):
    # key: 0 = C, 1 = C#/Db, 2 = D, 3 = D#/Eb, 4 = E, 5 = F, 6 = F#/Gb, 7 = G, 8 = G#/Ab, 9 = A, 10 = A#/Bb, 11 = B
    # mode: 1 = Major, 0 = Minor
    camelot_major = {
        0: "8B", 1: "3B", 2: "10B", 3: "5B", 4: "12B", 5: "7B", 
        6: "2B", 7: "9B", 8: "4B", 9: "11B", 10: "6B", 11: "1B"
    }
    camelot_minor = {
        0: "5A", 1: "12A", 2: "7A", 3: "2A", 4: "9A", 5: "4A", 
        6: "11A", 7: "6A", 8: "1A", 9: "8A", 10: "3A", 11: "10A"
    }
    
    # Nombre musical tradicional
    key_names = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    
    if key == -1:
        return "Unknown", "Unknown"
        
    musical_name = f"{key_names[key]} {'Mayor' if mode == 1 else 'Menor'}"
    camelot = camelot_major.get(key) if mode == 1 else camelot_minor.get(key)
    return musical_name, camelot

def test_10_songs():
    if "PEGAR" in SPOTIPY_CLIENT_ID:
        print("❌ ERROR: Necesito que pegues tu Client ID y Secret en el archivo spotify_scraper.py primero.")
        return

    print("✅ Autenticando con Spotify API Premium...")
    sp = spotipy.Spotify(auth_manager=SpotifyClientCredentials(
        client_id=SPOTIPY_CLIENT_ID,
        client_secret=SPOTIPY_CLIENT_SECRET
    ))

    # Vamos a buscar 10 canciones de Bad Bunny para la prueba
    artist_name = "Bad Bunny"
    print(f"\n🔍 Buscando 10 canciones top de {artist_name}...")
    
    results = sp.search(q=f"artist:{artist_name}", type='track', limit=10)
    tracks = results['tracks']['items']
    
    if not tracks:
        print("No se encontraron canciones.")
        return

    # Extraer los IDs de las canciones para pedir las Audio Features (BPM, Key, etc)
    track_ids = [track['id'] for track in tracks]
    features = sp.audio_features(tracks=track_ids)

    print("\n📊 RESULTADOS (10 Canciones de prueba):")
    print("-" * 80)
    
    for i in range(len(tracks)):
        track = tracks[i]
        feat = features[i]
        
        title = track['name']
        artist = track['artists'][0]['name']
        
        if feat:
            bpm_base = round(feat['tempo'])
            half_time = bpm_base / 2
            double_time = bpm_base * 2
            
            musical_key, camelot = get_camelot_key(feat['key'], feat['mode'])
            time_sig = feat['time_signature']
            energy = round(feat['energy'] * 100)
            danceability = round(feat['danceability'] * 100)
            
            print(f"🎵 {i+1}. {title} - {artist}")
            print(f"   BPM: {bpm_base} | Half: {half_time} | Double: {double_time}")
            print(f"   Key: {musical_key} (Camelot: {camelot}) | Compás: {time_sig}/4")
            print(f"   Energía: {energy}% | Bailabilidad: {danceability}%\n")
        else:
            print(f"🎵 {i+1}. {title} - {artist} (Sin métricas disponibles)\n")

if __name__ == "__main__":
    test_10_songs()
