from rap.utils.tornado_middleware import BaseHandler

import logging
log = logging.getLogger(__name__)


class HistoryAccess(BaseHandler):
    def get(self, *args, **kwargs):
        try:
            log.info("handling request")
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
