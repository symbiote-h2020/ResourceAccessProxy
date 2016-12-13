from configparser import RawConfigParser
from enum import Enum, IntEnum
from . import json_encode_pretty


def to_bool(s:str):
    """convert a string to boolean.

    A number of common strings are matched.
    For example, "Yes", "y", "ON", "true", "1" will all yield True.

    :param s: boolean representation string
    :return: boolean value
    """
    return RawConfigParser.BOOLEAN_STATES.get(s.lower())


def valid_enum(e: Enum):
    """Generator of Enum-based validators.

    The returned validators will check whether or not the passed value matches an entry of the specified enum,
    and return that enum entry.

    Support is provided for Enum and IntEnum objects.
    The passed values must be int-castable in case of an IntEnum.

    :param e: enum to specialize the validator
    :return: a validator object
    """

    def validate_int_enum(s:str):
        try:
            s = int(s)
            return e[s]
        except:
            raise ValueError("invalid %r term: %r" % (e, s))

    def validate_str_enum(s:str):
        try:
            return e[s]
        except:
            raise ValueError("invalid %r term: %r" % (e, s))

    if issubclass(e, IntEnum):
        return validate_int_enum
    return validate_str_enum


def valid_enum_str(e: Enum):
    """Generator of Enum-based validators.

    The returned validators will check whether or not the passed value matches an entry of the specified enum,
    and return that enum entry.

    :param e: enum to specialize the validator
    :return: a validator object
    """

    def validate_str_enum(s:str):
        try:
            return e[s]
        except:
            raise ValueError("invalid %r term: %r" % (e, s))

    return validate_str_enum


class JSONData(object):
    """map a dict hierarchy into to Python objects"""

    def __init__(self, d=None, **kwargs):
        if d is None:
            d = kwargs
        for a, b in d.items():
            if isinstance(b, (list, tuple)):
                self.__dict__[a] = [JSONData(x) if isinstance(x, dict) else x for x in b]
            else:
                self.__dict__[a] = JSONData(b) if isinstance(b, dict) else b

    def __getattr__(self, item):
        return self.__dict__.get(item)

    @staticmethod
    def __enc_elem(e):
        if isinstance(e, JSONData):
            return e.json
        if isinstance(e, list):
            return [JSONData.__enc_elem(i.json) for i in e]
        if isinstance(e, dict):
            res = {}
            for k, v in e.items():
                res[k] = JSONData.__enc_elem(v)
            return res
        try:
            json_encode_pretty(e)
            return e
        except TypeError:
            return "<%s@%r>" % (e.__class__.__name__, id(e))

    @property
    def json(self):
        res = {}
        for k, val in self.__dict__.items():
            res[k] = JSONData.__enc_elem(val)
        return res

    def __repr__(self):
        return json_encode_pretty(self.json)

