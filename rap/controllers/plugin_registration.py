import json
from tornado.ioloop import IOLoop
from rap.database.db import add_platform_plugin

import logging
log = logging.getLogger(__name__)


class PluginRegistration:
    @staticmethod
    def receive_message(ch, method, properties, body):
        key = method.routing_key
        log.debug("Plugin Registration message received.\nTopic: %s\nBody: %s", key, body)
        decoded = body.decode("utf-8")
        data = json.loads(decoded)
        platform_id = data.get("platformId")
        if key == "symbiote.rap.add-plugin":
            log.info("Registering plugin for platform %s", platform_id)
            add_platform_plugin(platform_id)
