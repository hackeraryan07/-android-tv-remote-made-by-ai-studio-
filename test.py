import urllib.request
import json
req = urllib.request.Request('https://api.github.com/search/code?q=PairingRequest+OuterMessage', headers={'User-Agent': 'Mozilla/5.0'})
resp = urllib.request.urlopen(req)
print(resp.read().decode('utf-8'))
