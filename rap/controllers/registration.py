import logging
log = logging.getLogger(__name__)

import json
from tornado.gen import coroutine
from tornado.ioloop import IOLoop


class ResourceRegistration:
    def __init__(self):
        self.io_loop = IOLoop.current()

    @coroutine
    def sendMonitorNotif(self, resourceId):
        log.debug("Sending monitoring notification for resource with id %s", id)


    def receiveMessage(self, ch, method, properties, body):
        log.debug("ResourceRegistration received %s", body)
        decoded = body.decode("utf-8")
        data = json.loads(decoded)

        #self.io_loop.add_callback(self.sendMonitorNotif, resourceId)

