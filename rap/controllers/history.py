import logging
log = logging.getLogger(__name__)

from utils.tornado_middleware import BaseHandler


class HistoryAccess(BaseHandler):
    def get(self, resourceId):
        try:
            log.info("handling request")
            # if ok
            #self.send({
            #    "status": "accepted",
            #})
        except Exception as err:
            log.exception("Exception while handling request")
            self.set_status(400)
            #self.send({
            #    "error_code": "bad_request",
            #    "error_msg": str(err)
            #})
