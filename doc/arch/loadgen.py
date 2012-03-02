#!/usr/bin/python
from signal import SIGTERM, signal, SIGINT

import pika
import random
import json
import hashlib
import sys
import os
import time


def msg():
    msg = {}
    msg['id']             = hashlib.sha1(os.urandom(20)).hexdigest()
#    msg['occurredMillis'] = random.randrange(1293840000, 1325376000) * 1000
#    msg['receivedMillis'] = random.randrange(1293840000, 1325376000) * 1000
    msg['occurredMillis']  =  int(time.time() * 1000)
    msg['receivedMillis']  =  int(time.time() * 1000)
    msg['userId']         = str(random.randrange(1, 1000))
    msg['clientId']       = str(random.randrange(1, 4))
    msg['resource']       = random.sample(("bandwidthup", "bandwidthdown",
                                           "vmtime", "diskspace"), 1)[0]
    msg['eventVersion']   = str(1)

    details = {}
    if msg['resource'] == 'vmtime':
        msg['value']    = random.randrange(0, 2)
        details['vmid'] = str(random.randrange(1, 4000))
    else:
        msg['value'] = random.randrange(1, 5000)
    msg['details'] = details
    return msg

def parse_arguments(args):
    from optparse import OptionParser

    parser = OptionParser()
    parser.add_option("-p", "--passwd", dest="passwd", help="Password")
    parser.add_option("-r", "--rate", dest="rate", type="int",
                      help="Number of msgs/sec", default = 5)
    return parser.parse_args(args)


def exit_handler(signum, frame):
    print "Number of messages: %d" % num_msgs
    connection.close()
    exit

(opts, args) = parse_arguments(sys.argv[1:])

credentials = pika.PlainCredentials('aquarium', opts.passwd)
parameters = pika.ConnectionParameters(
    host = 'aquarium.dev.grnet.gr',
    credentials = credentials)
connection = pika.BlockingConnection(parameters)

channel = connection.channel()
channel.exchange_declare(exchange='aquarium', passive = True)
connection.set_backpressure_multiplier(1000)

random.seed(0xdeadbabe)

num_msgs = 0
# signal(SIGTERM, exit_handler)
# signal(SIGINT, exit_handler)
oldtime = time.time() * 1000
old_messages = 0

while True:
    foo = msg()
    print "QUEUE %s %d" % (foo['id'], time.time() * 1000) 
    # Construct a message and send it
    #print json.dumps(foo)
    channel.basic_publish(exchange='aquarium',
                      routing_key='resevent.1.%s' % foo['resource'],
                      body=json.dumps(foo),
                      properties=pika.BasicProperties(
                          content_type="text/plain",
                          delivery_mode=1))
    num_msgs += 1
    newtime = time.time() * 1000

    if newtime - oldtime < 1000:
        if num_msgs - old_messages >= opts.rate:
            toSleep = float(1000 - newtime + oldtime)
            #print "msgs: %d sleeping for %f" % (num_msgs, toSleep)
            time.sleep(toSleep / 1000)
            oldtime = newtime
            old_messages = num_msgs
    else:
        oldtime = newtime
        old_messages = num_msgs
