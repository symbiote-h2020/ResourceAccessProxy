import pika
from registration import ResourceRegistration
from threading import Thread

import logging
log = logging.getLogger(__name__)


RAP_QUEUE = 'rap-queue'
RAP_EXCHANGE = "rap-exchange"
RAP_BINDINGS = {"symbiote.rap.register", "symbiote.rap.unregister"}

connection = None
channel = None


def connect_to_queue():
    global connection
    global channel
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    channel = connection.channel()


def configure_sender(exchange_name):
    global channel
    channel.exchange_declare(exchange=exchange_name, type='topic')


def configure_receiver(exchange, queue, keys):
    global channel
    registration = ResourceRegistration
    channel.exchange_declare(exchange=exchange, exchange_type='direct',
                             auto_delete=False, durable=False)
    result = channel.queue_declare(queue=queue, auto_delete=True)
    queue_name = result.method.queue
    for binding_key in keys:
        channel.queue_bind(exchange=exchange,
                           queue=queue_name,
                           routing_key=binding_key)

    channel.basic_consume(registration.receive_message,
                          queue=queue_name,
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
