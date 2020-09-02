import RPi.GPIO as GPIO
import time
import requests
import json
import sys

DHTPIN = 17

GPIO.setmode(GPIO.BCM)

DEVICE_NAME="atcdth11" # device name
EVENT_URL="http://localhost:48080/api/v1/event" #modify localhost to your EdgeXfoundry host

MAX_UNCHANGE_COUNT = 100

STATE_INIT_PULL_DOWN = 1
STATE_INIT_PULL_UP = 2
STATE_DATA_FIRST_PULL_DOWN = 3
STATE_DATA_PULL_UP = 4
STATE_DATA_PULL_DOWN = 5

def read_dht11_dat():
    GPIO.setup(DHTPIN, GPIO.OUT)
    GPIO.output(DHTPIN, GPIO.HIGH)
    time.sleep(0.05)
    GPIO.output(DHTPIN, GPIO.LOW)
    time.sleep(0.02)
    GPIO.setup(DHTPIN, GPIO.IN, GPIO.PUD_UP)

    unchanged_count = 0
    last = -1
    data = []
    while True:
        current = GPIO.input(DHTPIN)
        data.append(current)
        if last != current:
            unchanged_count = 0
            last = current
        else:
            unchanged_count += 1
            if unchanged_count > MAX_UNCHANGE_COUNT:
                break

    state = STATE_INIT_PULL_DOWN

    lengths = []
    current_length = 0
    for current in data:
        current_length += 1

        if state == STATE_INIT_PULL_DOWN:
            if current == GPIO.LOW:
                state = STATE_INIT_PULL_UP
            else:
                continue
        if state == STATE_INIT_PULL_UP:
            if current == GPIO.HIGH:
                state = STATE_DATA_FIRST_PULL_DOWN
            else:
                continue
        if state == STATE_DATA_FIRST_PULL_DOWN:
            if current == GPIO.LOW:
                state = STATE_DATA_PULL_UP
            else:
                continue
        if state == STATE_DATA_PULL_UP:
            if current == GPIO.HIGH:
                current_length = 0
                state = STATE_DATA_PULL_DOWN
            else:
                continue
        if state == STATE_DATA_PULL_DOWN:
            if current == GPIO.LOW:
                lengths.append(current_length)
                state = STATE_DATA_PULL_UP
            else:
                continue
    if len(lengths) != 40:
        #print "Data not good, skip"
        return False

    shortest_pull_up = min(lengths)
    longest_pull_up = max(lengths)
    halfway = (longest_pull_up + shortest_pull_up) / 2
    bits = []
    the_bytes = []
    byte = 0

    for length in lengths:
        bit = 0
        if length > halfway:
            bit = 1
        bits.append(bit)
    #print "bits: %s, length: %d" % (bits, len(bits))
    for i in range(0, len(bits)):
        byte = byte << 1
        if (bits[i]):
            byte = byte | 1
        else:
            byte = byte | 0
        if ((i + 1) % 8 == 0):
            the_bytes.append(byte)
            byte = 0
    #print the_bytes
    checksum = (the_bytes[0] + the_bytes[1] + the_bytes[2] + the_bytes[3]) & 0xFF
    if the_bytes[4] != checksum:
        return False

    return the_bytes[0], the_bytes[2]

def senddata(data):
    humidity,temperature = data
    event_url=EVENT_URL
    headers = {'Content-type': 'application/json'}
    postdata = {
        "device":DEVICE_NAME,
        "readings":[
            {
                "name": "Humidity",       # Sensor data type
                "value": str(humidity)    # Sensor data
            },
            {
                "name": "Temperature",    # Sensor data type
                "value": str(temperature) # Sensor data
            }]
    }
    response = requests.post(event_url,data=json.dumps(postdata), headers=headers)
    if response.status_code == 200:
        print("push event to edgexfoundry: Humidity: %s %%, Temperature: %s C" % (humidity,temperature))
        sys.stdout.flush()
    else:
        print("Failed to push data %s" % response.status_code)
        sys.stdout.flush()

def main():
    print("Raspberry Pi wiringPi DHT11 Temperature test program\n")

    sys.stdout.flush()
    count=0
    okresult=None
    while True:
        result = read_dht11_dat()
        count=(count+1)%15
        if result:
            okresult=result
            humidity, temperature = result
            print("humidity: %s %%,  Temperature: %s C`" % (humidity, temperature))
            sys.stdout.flush()

        if count==1:
            #send data
            if okresult:
               senddata(okresult)
        time.sleep(1)

def destroy():
    GPIO.cleanup()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        destroy()
