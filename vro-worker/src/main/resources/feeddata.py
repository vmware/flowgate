#!/usr/bin/python
# -*- coding: utf-8 -*-
from time import time
import requests
import random
from decimal import Decimal
import json
header = {'Content-Type': 'application/json'}
currentMiliseconds = int(round(time()*1000))
for i in range(0,5):
    temp = "%0.2f" % random.uniform(15.0,45.0)
    print temp
    data = {'assetID':'5acad7d6fac18d6808bbb8cb', 'value': temp,  'unit': 'Celsius', 'time': currentMiliseconds+i*10000 }
    print data
    r= requests.post("http://localhost:49610/v1/assets/5acad7d6fac18d6808bbb8cb/sensordata", headers=header, data=json.dumps(data))
    print r.status_code

