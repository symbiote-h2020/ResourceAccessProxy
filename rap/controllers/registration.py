import json
from tornado.gen import coroutine
from tornado.ioloop import IOLoop
from rap.database.db import check_platform, add_resource, delete_resource
import logging
log = logging.getLogger(__name__)


class ResourceRegistration:
    def __init__(self):
        self.io_loop = IOLoop.current()

    @coroutine
    def notify_monitoring(self, resource_id):
        log.debug("Sending monitoring notification for resource with id %s", resource_id)

    @staticmethod
    def receive_message(self, ch, method, properties, body):
        key = method.routing_key
        log.debug("Resource Registration message received.\nTopic: %s\nBody: %s", key, body)

        if not check_platform():
            raise Exception("This platform has not any plugin registered")

        decoded = body.decode("utf-8")
        data = json.loads(decoded)
        platform_id = data.get("platform_id")

        if key == "symbiote.rap.register":
            resource_id = data.get("resource_id")
            local_id = data.get("local_id")
            log.debug("Registering resource for platform %s with id %s", platform_id, resource_id)
            add_resource(resource_id, platform_id, local_id)
        elif key == "symbiote.rap.unregister":
            resource_id = data.get("resource_id")
            log.debug("Unregistering resource for platform %s with id %s", platform_id, resource_id)
            delete_resource(resource_id)

        #self.io_loop.add_callback(self.notify_monitoring, resource_id)

