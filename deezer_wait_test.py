import requests
import json
import urllib.parse
import time

ARL = "24f0c28bb6b2250db18693a312c11451126061c05d33aa8a4dcdeb2f9c8af3c6091ae6ae9ddfc2033399f5dfa66f93cae00ed26d05fecb4e0219f6a134b79b11b73712cb1d025c9789c6f2cbd34db3919b1688798024976afc259b8e526cd47f"

def test():
    session = requests.Session()
    session.cookies.set("arl", ARL, domain=".deezer.com")
    res = session.post("https://www.deezer.com/ajax/gw-light.php?method=deezer.getUserData&api_version=1.0&api_token=")
    data = res.json()["results"]
    api_token = data["checkForm"]
    license_token = data["USER"]["OPTIONS"]["license_token"]
    
    print("Esperando 360 segundos (6 minutos) para probar expiracion de token...")
    time.sleep(360)
    
    url = f"https://api.deezer.com/search?q={urllib.parse.quote('Bad Bunny Diles')}&limit=1"
    res = session.get(url)
    track_id = res.json()["data"][0]["id"]
    
    print("Re-obteniendo stream URL con token antiguo...")
    
    pdata_res = session.post(f"https://www.deezer.com/ajax/gw-light.php?method=song.getData&api_version=1.0&api_token={api_token}&input=3", json={"sng_id": str(track_id)}).json()
    track_token = pdata_res["results"].get("TRACK_TOKEN")
    if not track_token:
        print("Fallo obteniendo track token (api_token expiro). Esto activaria el refresco.")
        return
        
    payload = {"license_token": license_token, "media": [{"type": "FULL", "formats": [{"cipher": "BF_CBC_STRIPE", "format": "MP3_128"}]}], "track_tokens": [track_token]}
    res2 = session.post("https://media.deezer.com/v1/get_url", json=payload)
    
    print("Resultado despues de 6 mins:", res2.json())

test()
