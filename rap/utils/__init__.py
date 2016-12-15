__all__ = [
    "json_decode",
    "json_encode",
    "static_var",
]

# using AnyJSON to dynamically select the fastest JSON process available
from anyjson import loads as json_decode, dumps as __json_encode
from json import dumps as python_json_encode
from tornado.options import define
from datetime import datetime
from iso8601 import parse_date
from calendar import timegm

def utcnow():
    return datetime.now()

_EPOCH = datetime(1970, 1, 1)

def to_timet(txt):
    if isinstance(txt, datetime):
        dt = txt
    else:
        dt = parse_date(txt)
    return (dt - _EPOCH).total_seconds()

def static_var(varname, value=None):
    """A decorator to add static variables function objects.

    This decorator adds a named attribute to a function object,
    so that it can effectively be used as a static variable.

    Example:

    @static_var("goofy", 123)
    def test_me():
        test_me.goofy += 1
        print(test_me.goofy) # => 124
    """
    def decorate(func):
        setattr(func, varname, value)
        return func
    return decorate


def json_encode(*a, **ka):
    """Extract a JSON representation of an object.

    This function wraps a lower level encoder function which is dynamically picked by anyjson
    (using the fastest JSON codec available).

    :return: string data
    """
    return __json_encode(*a, **ka)

def json_encode_pretty(*a, **ka):
    """Extract a nicely indented JSON representation of an object.

    This function is guaranteed to use Python's system json.encode function,
    specifying a indent=4 parameter.
    Its output is a nicely indented, key-sorted, printable string.

    Specifying the pretty_json configuration parameter will force all JSON output
    to be nicely indented (introducing a noticeable performance drop).

    Useful for debugging.

    :return: string data
    """
    ka.update(indent=4, sort_keys=True)
    return python_json_encode(*a, **ka)


def __set_json_encoder(pretty):
    if pretty:
        global __json_encode
        __json_encode = json_encode_pretty

define("pretty_json", default=False, type=bool, help="Produce easily readable JSON output", metavar="BOOL", callback=__set_json_encoder)


class singleton:
    """
    A non-thread-safe helper class to ease implementing singletons.
    This should be used as a decorator -- not a metaclass -- to the
    class that should be a singleton.

    The decorated class can define one `__init__` function that
    takes only the `self` argument. Other than that, there are
    no restrictions that apply to the decorated class.

    To get the singleton instance, use the `Instance` method. Trying
    to use `__call__` will result in a `TypeError` being raised.

    Limitations: The decorated class cannot be inherited from.

    """

    def __init__(self, decorated):
        self._decorated = decorated

    def instance(self):
        """
        Returns the singleton instance. Upon its first call, it creates a
        new instance of the decorated class and calls its `__init__` method.
        On all subsequent calls, the already created instance is returned.

        """
        try:
            return self._instance
        except AttributeError:
            self._instance = self._decorated()
            return self._instance

    def __call__(self):
        raise TypeError('Singletons must be accessed through `instance()`.')

    def __instancecheck__(self, inst):
        return isinstance(inst, self._decorated)