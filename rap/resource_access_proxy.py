from tornado.ioloop import IOLoop
from tornado.options import define, options
from tornado.web import Application

from utils.tornado_middleware import SwaggerUIHandler
from pkg_resources import resource_filename
from rap.controllers import handlers as hdl
from rap.database.db import *
from rap.controllers.messaging_queue import MessagingQueue
from rap.controllers.messaging_queue import RAP_BINDINGS, RAP_EXCHANGE, RAP_QUEUE

import logging
log = logging.getLogger(__name__)


define("server_port", default=8080, type=int, help="HTTP web service port", metavar="TCP_PORT")
define("server_addr", default="localhost", type=str, help="HTTP web service bind address", metavar="IP_ADDR")
define("debug", default=False, type=bool, help="debug mode (not for production!!)", metavar="BOOL")


def make_application():
    handlers = [
        (r"/v1/resource/([0-9]+$)", hdl.ResourceAccess),
        (r"/v1/resource/([0-9]+)/(history)$", hdl.ResourceAccess)
    ]
    if options.debug:
        SwaggerUIHandler.add_at(handlers, "/docs", resource_filename("doc", "api_rap_0.1.0.yaml"))

    app = Application(handlers, debug=options.debug)
    log.debug("Setting up web service HTTP server at %s, port %d", options.server_addr, options.server_port)
    app.listen(options.server_port, options.server_addr)

    messaging_queue = MessagingQueue()
    queue_name = messaging_queue.configure_receiver(RAP_EXCHANGE, RAP_QUEUE, RAP_BINDINGS)
    log.debug('RabbitMQ service configured, waiting for messages on queue %s for topic %s', queue_name, RAP_EXCHANGE)

    log.info("Resource Access Proxy service started")

    return app


def main():
    from tornado.options import parse_config_file, parse_command_line
    define("config", type=str, help="path to config file", callback=lambda path: parse_config_file(path, final=False))
    parse_command_line()
    connect_to_db()
    log.info("Connecting to local database")
    ###########################################
    # these lines are only FOR TEST PURPOSES
    # inizialize_table()
    # initialize_resources()
    # select_all()
    # print("#### Get all resources of platform 'Works' ####")
    # get_resources(None, None, "Works");
    # print("#### Add resource 77 of platform 'Works' ####")
    # add_resource(77, "Works");
    # print("#### Get all resources of platform 'Works' ####")
    # get_resources(None, None, "Works");
    ###########################################
    app = make_application()
    IOLoop.instance().start()


if __name__ == "__main__":
    main()
