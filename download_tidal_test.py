import requests
import base64
import json
import urllib.parse
import os

from audio_metadata_injector import AudioMetadataInjector


def pad_base64(b64_str):
    return b64_str + "=" * ((4 - len(b64_str) % 4) % 4)


def get_cover_url(cover_id: str, size: int = 1280) -> str:
    """Build the Tidal CDN cover art URL from the cover UUID."""
    if not cover_id:
        return ""
    return f"https://resources.tidal.com/images/{cover_id.replace('-', '/')}/{size}x{size}.jpg"


def download_cover_art(cover_url: str) -> bytes | None:
    """Download the album cover and return raw JPEG bytes, or None on failure."""
    if not cover_url:
        return None
    try:
        resp = requests.get(cover_url, timeout=15)
        resp.raise_for_status()
        print(f"  Cover art downloaded ({len(resp.content):,} bytes)")
        return resp.content
    except Exception as e:
        print(f"  Warning: could not download cover art — {e}")
        return None


def main():
    refresh_token = "eyJraWQiOiJoUzFKYTdVMCIsImFsZyI6IkVTNTEyIn0.eyJ0eXBlIjoibzJfcmVmcmVzaCIsInVpZCI6MjA0MTg4NTU1LCJzY29wZSI6IndfdXNyIHJfdXNyIHdfc3ViIiwiY2lkIjoxMzMxOSwic1ZlciI6MSwiZ1ZlciI6MCwiaXNzIjoiaHR0cHM6Ly9hdXRoLnRpZGFsLmNvbS92MSJ9.ALlkbro7NIpyKNrtjCrh2_lqrxJIMUURSzLCi3KlqY7MTwAV9VO7-O4qbzog8AekvHKFf4l0HWgqD8OJk-YKlS_yAeBdhtxuY8bv_SdAcYdptgXOwYecdgGqIlPdTEobsgbyQ-105AN5Tu24MP8DG7qGgd24kzEmN2fQ5Jfs6A5w8LgH"
    client_id = "fX2JxdmntZWK0ixT"
    client_secret = "1Nn9AfDAjxrgJFJbKNWLeAyKGVGmINuXPPLHVXAvxAg="

    auth_string = f"{client_id}:{client_secret}"
    encoded_auth = base64.b64encode(auth_string.encode("utf-8")).decode("utf-8")

    # ── 1. Get access token ──────────────────────────────────────────────────
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

    # ── 2. Search for track ──────────────────────────────────────────────────
    print("Searching for 'Danny Ocean Corazón'...")
    query = "Danny Ocean Corazón"
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
    track_id    = track.get("id")
    track_title = track.get("title", "Unknown Title")
    track_artist = track.get("artist", {}).get("name", "Unknown Artist")
    album_obj   = track.get("album", {})
    album_title = album_obj.get("title", "Unknown Album")
    cover_id    = album_obj.get("cover", "")
    print(f"Found track: {track_title} by {track_artist} — Album: {album_title} (ID: {track_id})")

    # ── 3. Get stream URL (quality waterfall) ───────────────────────────────
    print("Getting stream URL...")
    qualities = ["LOSSLESS", "HIGH", "LOW"]
    stream_url = None
    for quality in qualities:
        print(f"  Trying quality: {quality}")
        playback_url = (
            f"https://api.tidalhifi.com/v1/tracks/{track_id}/playbackinfopostpaywall"
            f"?audioquality={quality}&playbackmode=STREAM&assetpresentation=FULL&countryCode=US"
        )
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
                    print(f"  Stream URL obtained at quality: {quality}")
                    break
        else:
            print(f"  Quality {quality} failed: {playback_resp.status_code}")

    if not stream_url:
        print("Failed to get stream URL.")
        return

    # ── 4. Download audio stream ─────────────────────────────────────────────
    print("Downloading audio stream...")
    output_dir = os.path.join(os.getcwd(), "pruebas tidal")
    os.makedirs(output_dir, exist_ok=True)

    ext = "flac" if "flac" in stream_url.lower() else ("m4a" if "mp4" in stream_url.lower() else "mp3")
    safe_name = f"{track_artist} - {track_title}".replace("/", "_").replace("\\", "_")
    output_filename = f"{safe_name}.{ext}"
    output_path = os.path.join(output_dir, output_filename)

    with requests.get(stream_url, stream=True, timeout=60) as r:
        r.raise_for_status()
        with open(output_path, "wb") as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)
    print(f"  Audio saved to: {output_path}")

    # ── 5. Download high-res cover art ───────────────────────────────────────
    cover_url = get_cover_url(cover_id, size=1280)
    print(f"Downloading cover art from: {cover_url}")
    cover_bytes = download_cover_art(cover_url)

    # ── 6. Fetch Tidal lyrics (synced, plain-text fallback) ──────────────────
    lyrics_json_str = None
    print("Fetching lyrics from Tidal...")
    try:
        lyrics_url = f"https://api.tidalhifi.com/v1/tracks/{track_id}/lyrics?countryCode=US"
        lyrics_resp = requests.get(lyrics_url, headers=search_headers, timeout=15)
        if lyrics_resp.status_code == 200:
            lyrics_data = lyrics_resp.json()
            raw_lyrics  = lyrics_data.get("lyrics", "")
            if raw_lyrics:
                # Convert Tidal LRC-style lines → canonical JSON via injector
                injector_tmp = AudioMetadataInjector()
                lyrics_json_str = injector_tmp._normalize_lyrics(raw_lyrics) if "[" in raw_lyrics else None
                if not lyrics_json_str:
                    # Plain-text fallback: wrap as single unsynchronized entry
                    lyrics_json_str = json.dumps([{"time": 0, "text": raw_lyrics}],
                                                 ensure_ascii=False)
            print(f"  Lyrics fetched ({len(raw_lyrics)} chars)")
        else:
            print(f"  Lyrics not available (status: {lyrics_resp.status_code})")
    except Exception as e:
        print(f"  Warning: lyrics fetch failed — {e}")

    # ── 7. Inject all metadata via AudioMetadataInjector ────────────────────
    injector = AudioMetadataInjector()
    injector.inject(
        audio_path  = output_path,
        cover_bytes = cover_bytes,
        lyrics_json = lyrics_json_str,
        title       = track_title,
        artist      = track_artist,
        album       = album_title,
        bpm         = 0,   # BPM not returned by Tidal search; set manually if known
    )

    print(f"\n✅ Done! Self-sufficient file: {output_path}")


if __name__ == "__main__":
    main()
