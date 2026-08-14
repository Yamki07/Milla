import requests
import base64
import json
import urllib.parse
import os

def pad_base64(b64_str):
    return b64_str + "=" * ((4 - len(b64_str) % 4) % 4)

def main():
    refresh_token = "eyJraWQiOiJoUzFKYTdVMCIsImFsZyI6IkVTNTEyIn0.eyJ0eXBlIjoibzJfcmVmcmVzaCIsInVpZCI6MjA0MTg4NTU1LCJzY29wZSI6IndfdXNyIHJfdXNyIHdfc3ViIiwiY2lkIjoxMzMxOSwic1ZlciI6MSwiZ1ZlciI6MCwiaXNzIjoiaHR0cHM6Ly9hdXRoLnRpZGFsLmNvbS92MSJ9.ALlkbro7NIpyKNrtjCrh2_lqrxJIMUURSzLCi3KlqY7MTwAV9VO7-O4qbzog8AekvHKFf4l0HWgqD8OJk-YKlS_yAeBdhtxuY8bv_SdAcYdptgXOwYecdgGqIlPdTEobsgbyQ-105AN5Tu24MP8DG7qGgd24kzEmN2fQ5Jfs6A5w8LgH"
    client_id = "fX2JxdmntZWK0ixT"
    client_secret = "1Nn9AfDAjxrgJFJbKNWLeAyKGVGmINuXPPLHVXAvxAg="
    
    auth_string = f"{client_id}:{client_secret}"
    encoded_auth = base64.b64encode(auth_string.encode("utf-8")).decode("utf-8")
    
    print("Getting Tidal access token...")
    token_url = "https://auth.tidal.com/v1/oauth2/token"
    token_data = {
        "refresh_token": refresh_token,
        "client_id": client_id,
        "grant_type": "refresh_token"
    }
    token_headers = {
        "Authorization": f"Basic {encoded_auth}",
        "User-Agent": "Tidal/2.36.1 Android/10"
    }
    token_resp = requests.post(token_url, data=token_data, headers=token_headers)
    token_json = token_resp.json()
    access_token = token_json.get("access_token")
    if not access_token:
        print("Failed to get token:", token_resp.text)
        return
    
    print("Searching for 'Karol G Papasito'...")
    query = "Karol G Papasito"
    encoded_q = urllib.parse.quote(query)
    search_url = f"https://api.tidalhifi.com/v1/search?query={encoded_q}&limit=5&types=TRACKS&countryCode=US"
    search_headers = {
        "Authorization": f"Bearer {access_token}",
        "User-Agent": "Tidal/2.36.1 Android/10"
    }
    search_resp = requests.get(search_url, headers=search_headers)
    search_json = search_resp.json()
    tracks = search_json.get("tracks", {}).get("items", [])
    if not tracks:
        print("No tracks found.")
        return
    
    track = tracks[0]
    track_id = track.get("id")
    track_title = track.get("title")
    track_artist = track.get("artist", {}).get("name", "Unknown")
    print(f"Found track: {track_title} by {track_artist} (ID: {track_id})")
    
    print("Getting stream URL...")
    qualities = ["LOSSLESS", "HIGH", "LOW"]
    stream_url = None
    for quality in qualities:
        print(f"Trying quality: {quality}")
        playback_url = f"https://api.tidalhifi.com/v1/tracks/{track_id}/playbackinfopostpaywall?audioquality={quality}&playbackmode=STREAM&assetpresentation=FULL&countryCode=US"
        playback_resp = requests.get(playback_url, headers=search_headers)
        if playback_resp.status_code == 200:
            playback_json = playback_resp.json()
            manifest_str = playback_json.get("manifest")
            if manifest_str:
                decoded_manifest = base64.urlsafe_b64decode(pad_base64(manifest_str)).decode("utf-8")
                manifest_json = json.loads(decoded_manifest)
                urls = manifest_json.get("urls", [])
                if urls:
                    stream_url = urls[0]
                    break
        else:
            print(f"Quality {quality} failed: {playback_resp.status_code}")
    
    if not stream_url:
        print("Failed to get stream URL.")
        return
    
    print("Downloading stream...")
    output_dir = os.path.join(os.getcwd(), "pruebas tidal")
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    ext = "flac" if "flac" in stream_url else ("m4a" if "mp4" in stream_url else "mp3")
    output_filename = f"{track_artist} - {track_title}.{ext}".replace("/", "_").replace("\\", "_")
    output_path = os.path.join(output_dir, output_filename)
    
    with requests.get(stream_url, stream=True) as r:
        r.raise_for_status()
        with open(output_path, 'wb') as f:
            for chunk in r.iter_content(chunk_size=8192): 
                f.write(chunk)
                
    print(f"Downloaded successfully to {output_path}")

if __name__ == '__main__':
    main()
