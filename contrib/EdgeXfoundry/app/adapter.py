from flask import request
from api.flowgateapi import FlowgateClient
from flask import current_app
from flask import Flask
app = Flask(__name__)

@app.route('/event', methods=['POST'])
def create_event():

    data = request.get_json()
    current_app.logger.info(data)
    flclient = FlowgateClient()
    flclient.init_app(current_app)
    response = flclient.postMetric(data)

    return response

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)