import requests
import json
import time
from api.config import Config
from flask import Response
class FlowgateClient():
    device_type ={
        'atcdth11':{                      # EdgexFoundry registered device name
            'Humidity':{'unit':'%'},      # postdata's unit from DTH11 on Raspberry Pi
            'Temperature':{'unit':'C'}    # postdata's unit from DTH11 on Raspberry Pi
        }
    }

    current_token={}

    def init_app(self,app):
        self.host = Config.FLOWGATE_HOST
        self.username = Config.FLOWGATE_USER
        self.password = Config.FLOWGATE_PASSWORD
        self.app = app

    def get_flowgate_token(self):

        if bool(self.current_token):
            current_time = int(round(time.time()*1000))
            if self.current_token['expires_in'] - current_time > 600000:
                return self.current_token


        token_url = self.host+'/apiservice/v1/auth/token'
        data = {}
        data["userName"]=self.username
        data["password"]=self.password
        headers = {'Content-type': 'application/json'}
        response = requests.post(token_url,data=json.dumps(data), headers=headers,verify=False)
        if response.status_code == 200:
            self.current_token = response.json()
            return self.current_token
        return None

    def postMetric(self,data):
        token = self.get_flowgate_token()

        #get asset by name
        device_name = data['device']
        device_id = self.getAssetIDByName(device_name)
        if device_id is None:
            response = Response()
            response.status_code = 500
            response.set_data("Failed to query device: "+device_name)
            self.app.logger.info("Failed to query device: "+device_name)
            return response
        device_data = {'assetID':device_id,'time':data['created'],'values':[]}
        for temp_data in data['readings']:
            value_unit = {}
            value_unit['time']=temp_data['created']
            value_unit['key']=temp_data['name']
            value_unit['unit']=self.device_type[device_name][temp_data['name']]['unit']
            value_unit['valueNum']=float(temp_data['value'])
            value_unit['value']=temp_data['value']
            value_unit['extraidentifier']=temp_data['name']
            device_data['values'].append(value_unit)

        if not device_data['values']:
            response = Response()
            response.status_code = 500
            response.set_data("No Valid data readings ")
            # self.app.logger.info("No Valid data readings ")
            return response
        device_data['id']=str(device_id)+"_"+str(data['created'])
        #now we have the data prepared and ready for send to flowgate

        api_url = self.host + "/apiservice/v1/assets/"+device_id+"/sensordata"
        headers = {'Content-type': 'application/json'}
        headers["Authorization"] = "Bearer "+token['access_token']
        response = requests.post(api_url,data=json.dumps(device_data),headers=headers, verify=False)
        self.app.logger.info("Response status code :" +str(response.status_code) + " for URL: "+api_url)
        response = Response()
        response.status_code = 200
        return response

    def getAssetIDByName(self,name):
        token = self.get_flowgate_token()
        api_url = self.host + "/apiservice/v1/assets/name/"+name+"/"
        headers = {'Content-type': 'application/json'}
        headers["Authorization"] = "Bearer "+token['access_token']
        self.app.logger.info("Query device: "+name)
        response = requests.get(api_url,headers=headers,verify=False)
        if response.status_code == 200:
            if response.text:
                try:
                    data = response.json()
                except ValueError:
                    self.app.logger.info('No json return')
                    return None
                return data['id']
        return None