import urllib.request
req = urllib.request.Request('https://raw.githubusercontent.com/Tronze/androidtvremote2/main/androidtvremote2/protocol/polo_pb2.py')
try:
    resp = urllib.request.urlopen(req)
    print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
