#!/usr/bin/env python
import logging
log = logging.getLogger(__name__)

from tornado.ioloop import IOLoop
from tornado.options import define, options
from tornado.web import Application
from threading import Thread

from utils.tornado_middleware import SwaggerUIHandler
from pkg_resources import resource_filename
from . import handlers as hdl

import pika

RAP_QUEUE = 'rap-queue'
RAP_EXCHANGE = "rap-exchange"
RAP_BINDINGS = {"rap.register", "rap.unregister"}


def make_application():
    handlers = [
        (r"/v1/register/[/]{0,1}$", hdl.ResourceAccess),
        (r"/v1/register/[/]{0,1}$/history", hdl.HistoryAccess)
    ]
    if options.debug:
        SwaggerUIHandler.add_at(handlers, "/docs", resource_filename("doc", "api_rap_0.1.0.yaml"))
    app = Application(handlers, debug=options.debug)
    log.info("setting up web service HTTP server at %s, port %d", options.server_addr, options.server_port)
    app.listen(options.server_port, options.server_addr)

    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    channel = connection.channel()
    channel.exchange_declare(exchange=RAP_EXCHANGE,
                             exchange_type='direct', auto_delete=False, durable=False)
    result = channel.queue_declare(queue=RAP_QUEUE, auto_delete=True)
    queue_name = result.method.queue
    for binding_key in RAP_BINDINGS:
        channel.queue_bind(exchange=RAP_EXCHANGE,
                           queue=queue_name,
                           routing_key=binding_key)
    registration = hdl.ResourceRegistration()
    channel.basic_consume(registration.receiveMessage,
                          queue=queue_name,
                          no_ack=True)
    log.info('starting RabbitMQ service, waiting for messages on queue %s for topic %s', queue_name, SLA_EXCHANGE)
    rabbit_thr = Thread(target=channel.start_consuming)
    rabbit_thr.start()

    service_status.new_event("Policy Maker service started")

    return app


def main():
    from tornado.options import parse_config_file, parse_command_line
    define("config", type=str, help="path to config file", callback=lambda path: parse_config_file(path, final=False))
    parse_command_line()
    app = make_application()
    IOLoop.instance().start()


if __name__ == "__main__":
    main()
