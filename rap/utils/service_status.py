from dolfin.utils import utcnow
from dolfin.utils.backend.policy_repo_async_client import PolicyRepo_API_Client
from dolfin.utils.backend.optimizer_async_client import Optimizer_API_Client
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
        self.policy_repo = PolicyRepo_API_Client()
        self.optimizer = Optimizer_API_Client()

    def new_event(self, msg):
        self.events.append(Event(msg))
        log.info("EVENT: %s", self.events[-1].format)

