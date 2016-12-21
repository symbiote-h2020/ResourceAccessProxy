from rap.utils.tornado_middleware import BaseHandler
import messaging_queue
from rap.database.db import get_resource, check_platform

import logging
log = logging.getLogger(__name__)


class ResourceAccess(BaseHandler):
    def get(self, item_id=None, history=False):
        try:
            log.info("handling request for item with id %s", item_id)
            if not check_platform():
                raise Exception("This platform has not any plugin registered")

            res_params = get_resource(item_id)
            routing_key = "get." + res_params[0];
            payload = {
                "action": "get",
                "platformId": res_params[0],
                "resourceId": res_params[1]
            }
            messaging_queue.send_message(routing_key, payload)
            self.set_status(200)
            if history:
                log.info("is history")
                res_params = get_resource(item_id)
                routing_key = "history." + res_params[0];
                payload = {
                    "action": "history",
                    "platformId": res_params[0],
                    "resourceId": res_params[1]
                }
                messaging_queue.send_message(routing_key, payload)
                self.set_status(200)
        except Exception as err:
            log.exception("Exception while handling request")
            self.set_status(400)

    def post(self, item_id=None):
        try:
            log.info("handling request for item with id %s", item_id)
            if not check_platform():
                raise Exception("This platform has not any plugin registered")

            value = self.get_json_argument("value")
            res_params = get_resource(item_id)
            routing_key = "set." + res_params[0];
            payload = {
                "action": "set",
                "platformId": res_params[0],
                "resourceId": res_params[1],
                "value": value
            }
            messaging_queue.send_message(routing_key, payload)
            self.set_status(200)
        except Exception as err:
            log.exception("Exception while handling request")
            self.set_status(400)