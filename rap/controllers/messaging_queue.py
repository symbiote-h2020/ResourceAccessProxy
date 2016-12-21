import pika
from registration import ResourceRegistration
from plugin_registration import PluginRegistration
from threading import Thread

import logging
log = logging.getLogger(__name__)


RAP_EXCHANGE_IN = "rap-exchange"

NORTHBOUND_KEYS = {"symbiote.rap.register", "symbiote.rap.unregister"}
NORTHBOUND_QUEUE = "symbiote-queue"

PLATFORM_KEYS = {"symbiote.rap.add-plugin"}
PLATFORM_QUEUE = "platform-queue"

PLUGINS_EXCHANGE_OUT = "plugins-exchange"

connection = None
channel = None


def connect_to_queue():
    global connection
    global channel
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    channel = connection.channel()


def configure_sender():
    global channel
    channel.exchange_declare(exchange=PLUGINS_EXCHANGE_OUT, type='topic')


def configure_receiver():
    global channel
    channel.exchange_declare(exchange=RAP_EXCHANGE_IN, type='topic')

    registration = ResourceRegistration()

    result = channel.queue_declare(queue=NORTHBOUND_QUEUE, auto_delete=True)
    north_queue_name = result.method.queue
    for binding_key in NORTHBOUND_KEYS:
        channel.queue_bind(exchange=RAP_EXCHANGE_IN,
                           queue=NORTHBOUND_QUEUE,
                           routing_key=binding_key)
    channel.basic_consume(registration.receive_message,
                          queue=north_queue_name,
                          no_ack=True)

    result = channel.queue_declare(queue=PLATFORM_QUEUE, auto_delete=True)
    platform_queue_name = result.method.queue
    for binding_key in PLATFORM_KEYS:
        channel.queue_bind(exchange=RAP_EXCHANGE_IN,
                           queue=PLATFORM_QUEUE,
                           routing_key=binding_key)
    channel.basic_consume(PluginRegistration.receive_message,
                          queue=platform_queue_name,
                          no_ack=True)

    rabbit_thr = Thread(target=channel.start_consuming)
    rabbit_thr.start()


def send_message(routing_key, message):
    global channel
    channel.basic_publish(exchange='topic_logs',
                          routing_key=routing_key,
                          body=message)
    log.debug("Sent message:\n%r:%r" % (routing_key, message))


def close_connection_with_queue():
    global connection
    connection.close()
