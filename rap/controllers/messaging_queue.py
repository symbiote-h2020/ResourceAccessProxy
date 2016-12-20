import pika
from registration import ResourceRegistration
from threading import Thread

import logging
log = logging.getLogger(__name__)


RAP_QUEUE = 'rap-queue'
RAP_EXCHANGE = "rap-exchange"
RAP_BINDINGS = {"rap.register", "rap.unregister"}


class MessagingQueue:
    def __init__(self):
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
        self.channel = self.connection.channel()

    def configure_sender(self, exchange_name):
        self.channel.exchange_declare(exchange=exchange_name, type='topic')

    def configure_receiver(self, exchange, queue, keys):
        registration = ResourceRegistration
        self.channel.exchange_declare(exchange=exchange, exchange_type='direct',
                                      auto_delete=False, durable=False)
        result = self.channel.queue_declare(queue=queue, auto_delete=True)
        queue_name = result.method.queue
        for binding_key in keys:
            self.channel.queue_bind(exchange=exchange,
                                    queue=queue_name,
                                    routing_key=binding_key)

        self.channel.basic_consume(registration.receive_message,
                                   queue=queue_name,
                                   no_ack=True)

        rabbit_thr = Thread(target=self.channel.start_consuming)
        rabbit_thr.start()

        return queue_name

    def send_message(self, routing_key, message):
        self.channel.basic_publish(exchange='topic_logs',
                                   routing_key=routing_key,
                                   body=message)
        log.debug(" Sent message:\n%r:%r" % (routing_key, message))

    def close_connection(self):
        self.connection.close()
