from rap.utils.tornado_middleware import BaseHandler

import logging
log = logging.getLogger(__name__)


class ResourceAccess(BaseHandler):
    def get(self, item_id=None, history=False):
        try:
            log.info("handling request for item with id %s", item_id)
            # if ok
            #self.send({
            #    "status": "accepted",
            #})
            self.set_status(200)
            if history:
                log.info("is history")
                self.set_status(201)
        except Exception as err:
            log.exception("Exception while handling request")
            self.set_status(400)
            #self.send({
            #    "error_code": "bad_request",
            #    "error_msg": str(err)
            #})

    def post(self, item_id=None):
        try:
            log.info("handling request for item with id %s", item_id)
            # if ok
            #self.send({
            #    "status": "accepted",
            #})
            self.set_status(200)
        except Exception as err:
            log.exception("Exception while handling request")
            self.set_status(400)
            #self.send({
            #    "error_code": "bad_request",
            #    "error_msg": str(err)
            #})