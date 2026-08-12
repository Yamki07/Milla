import urllib.request
import json
import time
import random

SUPABASE_URL = "https://brgwlyixvgdvzahmsusf.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_4qGbvRV8ArCt3OkFe4mcCQ_r9DpCKM1"
ENDPOINT = f"{SUPABASE_URL}/rest/v1/track_metadata"

CHART_ENDPOINTS = [
    "chart/0/tracks?limit=100",    # Top Global
    "chart/23/tracks?limit=50",    # Top USA
    "chart/116/tracks?limit=50",   # Top Espania
    "chart/4/tracks?limit=50",     # Top Francia
    "chart/8/tracks?limit=50",     # Top Brasil
]

EDITORIAL_QUERIES = [
    "reggaeton 2025", "pop hits 2024",
    "afrobeats 2025", "latin hits"
]

def sanitize_string(s):
    return "".join(c for c in str(s).lower() if c.isalnum())

def generate_track_id(artist, title, deezer_id):
    clean_artist = sanitize_string(artist)[:10]
    clean_title = sanitize_string(title)[:15]
    return f"{clean_artist}_{clean_title}_dz{deezer_id}"

def fetch_deezer(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return json.loads(response.read().decode())
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None

def generate_advanced_profile(duration_ms, bpm):
    # Mocking DSP logic for AutoMix Level 2+
    duration_sec = duration_ms / 1000.0
    
    # 1. Beats & Downbeats
    bps = bpm / 60.0 if bpm > 0 else 2.0
    beat_interval = 1.0 / bps
    beats = [round(i * beat_interval, 3) for i in range(int(duration_sec / beat_interval))]
    downbeats = [beats[i] for i in range(0, len(beats), 4)]
    
    # 2. Sections
    sections = [
        {"start": 0.0, "end": 15.0, "label": "intro"},
        {"start": 15.0, "end": duration_sec - 20.0, "label": "verse_chorus"},
        {"start": duration_sec - 20.0, "end": duration_sec, "label": "outro"}
    ]
    
    # 3. Energy Curve
    energy_curve = [
        {"time": 0.0, "energy": 0.4},
        {"time": 15.0, "energy": 0.7},
        {"time": duration_sec / 2, "energy": 0.9},
        {"time": duration_sec - 20.0, "energy": 0.6},
        {"time": duration_sec, "energy": 0.2}
    ]
    
    # 4. Vocal Segments
    vocal_segments = [
        {"start": 15.0, "end": duration_sec - 20.0}
    ]
    
    # 5. Mix In / Out
    mix_in_points = [downbeats[1] if len(downbeats) > 1 else 0.0, downbeats[2] if len(downbeats) > 2 else 0.0]
    mix_out_points = [duration_sec - 15.0, duration_sec - 10.0]
    
    # 6. Struct
    intro_style = "instrumental"
    ending_type = "fade"
    
    profile = {
        "bpm": bpm,
        "beats": beats,
        "downbeats": downbeats,
        "sections": sections,
        "energy_curve": energy_curve,
        "vocal_segments": vocal_segments,
        "mix_in_points": mix_in_points,
        "mix_out_points": mix_out_points,
        "ending_type": ending_type,
        "intro_style": intro_style
    }
    return profile

def push_to_supabase(data):
    req = urllib.request.Request(
        ENDPOINT,
        data=json.dumps(data).encode('utf-8'),
        headers={
            'apikey': SUPABASE_ANON_KEY,
            'Authorization': f'Bearer {SUPABASE_ANON_KEY}',
            'Content-Type': 'application/json',
            'Prefer': 'resolution=merge-duplicates'
        },
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status in (200, 201)
    except urllib.error.HTTPError as e:
        print(f"Error uploading {data.get('track_id')}: {e.code} {e.reason}")
        return False
    except Exception as e:
        print(f"Error uploading {data.get('track_id')}: {e}")
        return False

def main():
    print("Iniciando Milla Supabase Crawler...")
    seeded_count = 0
    all_tracks = []
    
    for endpoint in CHART_ENDPOINTS:
        print(f"Fetching {endpoint}...")
        res = fetch_deezer(f"https://api.deezer.com/{endpoint}")
        if res and 'data' in res:
            all_tracks.extend(res['data'])
            
    for query in EDITORIAL_QUERIES:
        print(f"Fetching search '{query}'...")
        res = fetch_deezer(f"https://api.deezer.com/search?q={urllib.parse.quote(query)}&limit=30")
        if res and 'data' in res:
            all_tracks.extend(res['data'])
            
    # Deduplicate
    unique_tracks = {t['id']: t for t in all_tracks}.values()
    print(f"Found {len(unique_tracks)} unique tracks to process. Injecting to Supabase...")
    
    # Only process up to 150 for this demo to save time, randomly shuffled
    unique_list = list(unique_tracks)
    random.shuffle(unique_list)
    unique_list = unique_list[:150]
    
    for track in unique_list:
        track_id = track['id']
        title = track.get('title', '')
        artist = track.get('artist', {}).get('name', '')
        duration_sec = track.get('duration', 0)
        
        detail = fetch_deezer(f"https://api.deezer.com/track/{track_id}")
        time.sleep(0.1) # rate limit
        if not detail: continue
        
        bpm = float(detail.get('bpm', 0.0))
        if bpm == 0: bpm = random.uniform(85.0, 130.0) # mock missing bpm
        gain = float(detail.get('gain', 0.0))
        
        custom_id = generate_track_id(artist, title, track_id)
        full_json = generate_advanced_profile(duration_sec * 1000, bpm)
        
        payload = {
            "track_id": custom_id,
            "title": title,
            "artist": artist,
            "bpm": bpm,
            "musical_key": "",
            "cue_out_ms": int((duration_sec - 15) * 1000) if duration_sec > 15 else 0,
            "replay_gain": gain,
            "synced_lyrics": None,
            "full_profile_json": full_json
        }
        
        if push_to_supabase(payload):
            seeded_count += 1
            if seeded_count % 10 == 0:
                print(f"Seeded {seeded_count} tracks...")
                
    print(f"Crawler finalizado! Total inyectados: {seeded_count}")

if __name__ == '__main__':
    main()
