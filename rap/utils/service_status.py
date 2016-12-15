from . import utcnow

import logging
log = logging.getLogger(__name__)

class Event:
    def __init__(self, format):
        self.ts = utcnow()
        self.format = format

    def __str__(self):
        return self.format


class ServiceStatus:
    def __init__(self):
        self.events = []

    def new_event(self, msg):
        self.events.append(Event(msg))
        log.info("EVENT: %s", self.events[-1].format)

