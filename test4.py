import urllib.request
import json
req = urllib.request.Request('https://raw.githubusercontent.com/Tronze/androidtvremote2/main/src/androidtvremote2/pairing.py')
try:
    resp = urllib.request.urlopen(req)
    print(resp.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
