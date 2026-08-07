import requests
import json
import hashlib
import time
import urllib.parse
from Crypto.Cipher import AES
import concurrent.futures

ARL = "24f0c28bb6b2250db18693a312c11451126061c05d33aa8a4dcdeb2f9c8af3c6091ae6ae9ddfc2033399f5dfa66f93cae00ed26d05fecb4e0219f6a134b79b11b73712cb1d025c9789c6f2cbd34db3919b1688798024976afc259b8e526cd47f"

def get_session():
    session = requests.Session()
    session.headers.update({"User-Agent": "Mozilla/5.0"})
    session.cookies.set("arl", ARL, domain=".deezer.com")
    
    res = session.post("https://www.deezer.com/ajax/gw-light.php?method=deezer.getUserData&api_version=1.0&api_token=")
    data = res.json()["results"]
    api_token = data["checkForm"]
    license_token = data["USER"]["OPTIONS"]["license_token"]
    return session, api_token, license_token

def search(session, query):
    url = f"https://api.deezer.com/search?q={urllib.parse.quote(query)}&limit=2"
    res = session.get(url)
    return res.json()["data"]

def get_private_data(session, api_token, track_id):
    url = f"https://www.deezer.com/ajax/gw-light.php?method=song.getData&api_version=1.0&api_token={api_token}&input=3"
    payload = {"sng_id": str(track_id)}
    res = session.post(url, json=payload)
    return res.json()["results"]

def get_stream_url(session, license_token, track_token, format_id):
    payload = {
        "license_token": license_token,
        "media": [{"type": "FULL", "formats": [{"cipher": "BF_CBC_STRIPE", "format": format_id}]}],
        "track_tokens": [track_token]
    }
    res = session.post("https://media.deezer.com/v1/get_url", json=payload)
    data = res.json()
    try:
        return data["data"][0]["media"][0]["sources"][0]["url"]
    except Exception as e:
        print("Error getting URL:", data)
        return None

def get_track_key(track_id):
    md5 = hashlib.md5(str(track_id).encode()).hexdigest()
    secret = "g4el58wc0zvf9na1"
    key = "".join([chr(ord(md5[i]) ^ ord(md5[i+16]) ^ ord(secret[i])) for i in range(16)])
    return key.encode('latin1')

def get_musixmatch_lyrics(track, artist):
    try:
        url_token = "https://apic-desktop.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0"
        token = requests.get(url_token, headers={"User-Agent":"Mozilla/5.0"}).json()["message"]["body"]["user_token"]
        
        url = f"https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&q_track={urllib.parse.quote(track)}&q_artist={urllib.parse.quote(artist)}&user_language=en&f_subtitle_length_max_deviation=1&subtitle_format=mxm&app_id=web-desktop-app-v1.0&usertoken={token}"
        res = requests.get(url, headers={"User-Agent":"Mozilla/5.0"}).json()
        macro = res["message"]["body"]["macro_calls"]
        subs = macro.get("track.subtitles.get", {}).get("message", {}).get("body", {}).get("subtitle_list", [])
        if subs:
            return subs[0]["subtitle"]["subtitle_body"]
    except Exception as e:
        return str(e)
    return None

def download_and_decrypt(session, url, track_id, title):
    print(f"[{title}] Empezando descarga...")
    key = get_track_key(track_id)
    res = session.get(url, stream=True)
    with open(f"{title}.mp3", "wb") as f:
        chunk_idx = 0
        for chunk in res.iter_content(chunk_size=2048):
            if len(chunk) == 2048 and chunk_idx % 3 == 0:
                cipher = AES.new(key, AES.MODE_ECB)
                f.write(cipher.decrypt(chunk))
            else:
                f.write(chunk)
            chunk_idx += 1
    print(f"[{title}] Finalizado y desencriptado!")

def process_track(sess_data, track):
    session, api_token, license_token = sess_data
    track_id = track["id"]
    title = track["title"]
    artist = track["artist"]["name"]
    print(f"Procesando: {title} by {artist}")
    
    # 1. Fetch Musixmatch lyrics
    mxm = get_musixmatch_lyrics(title, artist)
    print(f"[{title}] Musixmatch Lyrics Extraidas: {'Si' if mxm else 'No'}")
    if mxm:
        print(f"[{title}] Fragmento: {mxm[:100]}...")
    
    # 2. Private Data
    pdata = get_private_data(session, api_token, track_id)
    track_token = pdata.get("TRACK_TOKEN")
    
    # 3. Stream URL
    url = get_stream_url(session, license_token, track_token, "MP3_128")
    if not url:
        print(f"[{title}] Error obteniendo Stream URL. El token funciona bien?")
        return
    print(f"[{title}] Stream URL Obtenida (Token media.deezer.com OK)")
    
    # 4. Descargar
    download_and_decrypt(session, url, track_id, title.replace("?", "").replace("/", ""))

if __name__ == "__main__":
    sess_data = get_session()
    tracks = search(sess_data[0], "Bad Bunny")
    
    print("Encontradas 2 canciones de Bad Bunny. Descargando al mismo tiempo...")
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        for t in tracks:
            executor.submit(process_track, sess_data, t)
    print("Prueba finalizada.")
