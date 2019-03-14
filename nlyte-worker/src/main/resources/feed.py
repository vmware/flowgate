#!/usr/bin/python
# -*- coding: utf-8 -*-
import time
import requests
import random
from decimal import Decimal
import json
header = {'Content-Type': 'application/json'}
count = 0
asset = {
  "assetNumber": 105077,
  "assetName": "cn-aurora-02",
  "assetSource": "0",
  "category": "Server",
  "subCategory": "Standard",
  "manufacturer": "Dell",
  "model": "PowerEdge R510",
  "serialnumber": "HCRCY2X",
  "tag": "76144",
  "region": "APAC",
  "country": "China",
  "city": "Beijing",
  "building": "S Wing of Tower C, Raycom InfoTech Park",
  "floor": "8th",
  "room": "Beijing Lab",
  "row": null,
  "col": null,
  "cabinetName": "R06",
  "cabinetUnitPosition": 14,
  "cabinetsize": 42,
  "cabinetAssetNumber": "101002",
  "lastupdate": 0
}
while():
    currentMiliseconds = int(round(time.time()*1000))
    for i in range(0,5):
        temp = "%0.2f" % random.uniform(35.0,40.0)
        print temp
        data = {'assetID':'5acad7d6fac18d6808bbb8cb', 'value': temp,  'unit': 'Celsius', 'time': currentMiliseconds+i*30000 }
        print data
        r= requests.post("http://localhost:49610/v1/assets/5acad7d6fac18d6808bbb8cb/sensordata", headers=header, data=json.dumps(data))
        print r.status_code
        temp2 ="%0.2f" % random.uniform(30.0,34.0)
        data2 = {'assetID':'5ae09522ac82f8121212fa5d', 'value': temp2, 'unit':'Celsius', 'time': currentMiliseconds+i*30000 }
        r=requests.post("http://localhost:49610/v1/assets/5ae09522ac82f8121212fa5d/sensordata", headers=header, data=json.dumps(data2))
        print r.status_code

        percent = "%0.2f" % random.uniform(75.0,76.0)
        data3 = {'assetID':'5ae6d414ac82f8c7cee6b506','value':percent, 'unit':'Percent','time': currentMiliseconds+i*30000 }
        r = requests.post("http://localhost:49610/v1/assets/5ae6d414ac82f8c7cee6b506/sensordata", headers=header, data=json.dumps(data3))
        print r.status_code
    count = count+1
    time.sleep(300)
