import json
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--inputs', dest='inputs')
args = parser.parse_args()

with open(args.inputs) as json_file:
    data = json.load(json_file)

# Do some work

myResult = {
    "commandResult": "success",
    "detectedIp": True,
    "ipList": [
        "0.0.0.0", "0.2.3.4"
    ],
    "originalVarsFromZeebe": data
}

print(json.dumps(myResult))
