import pika

import logging
log = logging.getLogger(__name__)


class QueueMessaging:
    def __init__(self, exchange_name):
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
        self.channel = self.connection.channel()
        self.channel.exchange_declare(exchange=exchange_name, type='topic')

    def send_message(self, routing_key, message):
        self.channel.basic_publish(exchange='topic_logs',
                                   routing_key=routing_key,
                                   body=message)
        log.debug(" Sent message:\n%r:%r" % (routing_key, message))

    def close_connection(self):
        self.connection.close()
