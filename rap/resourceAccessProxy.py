#!/usr/bin/env python
import logging
log = logging.getLogger(__name__)

from tornado.ioloop import IOLoop
from tornado.options import define, options
from tornado.web import Application
from threading import Thread

from utils.tornado_middleware import SwaggerUIHandler
from pkg_resources import resource_filename
from rap.controllers import handlers as hdl
from rap.utils.service_status import ServiceStatus

import pika

RAP_QUEUE = 'rap-queue'
RAP_EXCHANGE = "rap-exchange"
RAP_BINDINGS = {"rap.register", "rap.unregister"}

define("server_port", default=8080, type=int, help="HTTP web service port", metavar="TCP_PORT")
define("server_addr", default="localhost", type=str, help="HTTP web service bind address", metavar="IP_ADDR")
define("debug", default=False, type=bool, help="debug mode (not for production!!)", metavar="BOOL")


def configureQueue(channel, handler):
    channel.exchange_declare(exchange=RAP_EXCHANGE,
                             exchange_type='direct', auto_delete=False, durable=False)
    result = channel.queue_declare(queue=RAP_QUEUE, auto_delete=True)
    queue_name = result.method.queue
    for binding_key in RAP_BINDINGS:
        channel.queue_bind(exchange=RAP_EXCHANGE,
                           queue=queue_name,
                           routing_key=binding_key)

    channel.basic_consume(handler.receiveMessage,
                          queue=queue_name,
                          no_ack=True)
    return queue_name

def make_application():
    service_status = ServiceStatus()
    handlers = [
        (r"/v1/resource/([0-9]+$)", hdl.ResourceAccess),
        (r"/v1/resource/([0-9]+)/(history)$", hdl.ResourceAccess)
    ]
    registration = hdl.ResourceRegistration()
    if options.debug:
        SwaggerUIHandler.add_at(handlers, "/docs", resource_filename("doc", "api_rap_0.1.0.yaml"))
    app = Application(handlers, debug=options.debug)
    log.info("Setting up web service HTTP server at %s, port %d", options.server_addr, options.server_port)
    app.listen(options.server_port, options.server_addr)

    # connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    # channel = connection.channel()
    # queue_name = configureQueue(registration)
    # log.info('RabbitMQ service configured, waiting for messages on queue %s for topic %s', queue_name, RAP_EXCHANGE)
    #
    # rabbit_thr = Thread(target=channel.start_consuming)
    # rabbit_thr.start()

    service_status.new_event("Resource Access Proxy service started")

    return app


def main():
    from tornado.options import parse_config_file, parse_command_line
    define("config", type=str, help="path to config file", callback=lambda path: parse_config_file(path, final=False))
    parse_command_line()
    app = make_application()
    IOLoop.instance().start()


if __name__ == "__main__":
    main()
