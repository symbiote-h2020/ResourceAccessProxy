import logging
log = logging.getLogger(__name__)

import json
from tornado.gen import coroutine
from tornado.ioloop import IOLoop
from rap.database.db import add_resource, get_resources

class ResourceRegistration:
    def __init__(self):
        self.io_loop = IOLoop.current()

    @coroutine
    def notify_monitoring(self, resource_id):
        log.debug("Sending monitoring notification for resource with id %s", id)


    def receive_message(self, ch, method, properties, body):
        key = method.routing_key
        log.debug("Resource Registration message received.\nTopic: %s\nBody: %s", key, body)
        decoded = body.decode("utf-8")
        data = json.loads(decoded)

        #if not isinstance(action, str):
        #    "wrong value, it must be string"
        #    return

        if key == "rap.register":
            print "register"
        if key == "rap.unregister":
            print "unregister"

        #self.io_loop.add_callback(self.notify_monitoring, resource_id)

