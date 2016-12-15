__all__ = [
    "BaseHandler"
]

import logging

log = logging.getLogger(__name__)

from tornado.web import RequestHandler
from tornado.options import options
from os.path import join, isfile, basename
from . import json_decode, json_encode
from tornado.web import MissingArgumentError, StaticFileHandler, HTTPError
from pkg_resources import resource_filename
from tornado.escape import to_basestring


class BadArgumentError(ValueError):
    def __init__(self, arg_name):
        super(BadArgumentError, self).__init__(
            'Bad argument %s' % arg_name)
        self.arg_name = arg_name


class BaseHandler(RequestHandler):
    """a Tornado RequesHandler which is tightly coupled with the Deseeo props and dynamic model architecture"""

    def prepare(self):
        """switch scope internal status at HTTP session request begin"""
        self.set_cors_headers()

    def set_cors_headers(self):
        """return CORS headers, effective to allow interdomain API requests to go through.
        """
        self.set_header("Access-Control-Max-Age", "3600")
        origin = str(self.request.headers.get("Origin"))
        self.set_header("Access-Control-Allow-Origin", origin)
        self.set_header("Access-Control-Allow-Methods", "*")
        self.set_header("Access-Control-Allow-Credentials", "true")

    @property
    def json(self):
        if not hasattr(self, "_json"):
            if self.request.headers.get("content-type", "").startswith("application/json"):
                self._json = json_decode(to_basestring(self.request.body))
            else:
                self._json = None
        return self._json

    def options(self, *args, **kwargs):
        """all HTTP OPTIONS requests should always yield CORS"""
        self.set_cors_headers()

    def log_exception(self, *a, **ka):
        """fun fact: this method is always called when an exception is raised during within the handler.
        We exploit this to target those conditions involving an error notification"""
        return RequestHandler.log_exception(self, *a, **ka)

    def _param(self, getter, name, validator=None, default=RequestHandler._ARG_DEFAULT):
        try:
            val = getter(name, default, False)
        except MissingArgumentError:
            raise MissingArgumentError(name)
        if validator is None:
            return val
        try:
            return validator(val)
        except ValueError:
            raise BadArgumentError(name)

    def body_param(self, name, validator=None, default=RequestHandler._ARG_DEFAULT):
        """Retrieve a parameter from the requert body (usually for POST/PUT requests), encoded in FORM format.

        If validator is specified, this callable object must be able to parse the parameter string into
        the output value, or raise ValueError. A ValueError exception will in turn raise WRONG_PARAMETER.

        If default is not specified, this call may raise PARAMETER_EMPTY

        :param name: name of the parameter
        :param validator: validator callable object
        :param default: value to return if the parameter is not found
        :return: string, or the output value returned by validator
        """
        getter = self.json is not None and self.get_json_argument or self.get_body_argument
        return self._param(getter, name, validator, default)

    def url_param(self, name, validator=None, default=RequestHandler._ARG_DEFAULT):
        """Retrieve a parameter from the requert URI.

        If validator is specified, this callable object must be able to parse the parameter string into
        the output value, or raise ValueError. A ValueError exception will in turn raise WRONG_PARAMETER.

        If default is not specified, this call may raise PARAMETER_EMPTY

        :param name: name of the parameter
        :param validator: validator callable object
        :param default: value to return if the parameter is not found
        :return: string, or the output value returned by validator
        """
        return self._param(self.get_query_argument, name, validator, default)

    def get_json_argument(self, name, default=RequestHandler._ARG_DEFAULT, validator=None, strip=False):
        """get the value of a JSON request body field.

        Note: this only works if the quested body has been posted with a Content-Type header of type
        application/json, and only if such header is parsable as a valid JSON object.

        The argument fetch will always fail if the request body is non specified as JSON.

        :param name: name of the JSON object member
        :param default: value to return if the parameter is not found
        :return: the value associated with the JSON object member
        """
        args = self.json
        v = default
        if isinstance(args, dict):
            v = args.get(name, default)
            if strip and hasattr(v, "strip"):
                v = v.strip()
            if validator is not None:
                try:
                    v = validator(v)
                except ValueError:
                    raise BadArgumentError(name)
        if v is BaseHandler._ARG_DEFAULT:
            raise MissingArgumentError(name)
        return v

    def send(self, data):
        self.set_header("Content-Type", "application/json")
        self.finish(json_encode(data))

    def send_error(self, *a, **kwargs):
        on_managed_error = getattr(self, "on_managed_error", None)
        if not getattr(options, "debug", False) or not on_managed_error:
            return RequestHandler.send_error(self, *a, **kwargs)

        if 'exc_info' in kwargs:
            exception = kwargs['exc_info'][1]
            return on_managed_error(exception)
        log.warning("error condition not reported to client: %r, %r", a, kwargs)

    def get_session_id(self):
        """ OVERRIDE THIS: return a meaningful, secure session ID, using whatever source it may be needed.
        :return: current user's session id, as probably extracted from a secure cookie or an header, or None
        """
        pass


class SwaggerUIHandler(StaticFileHandler):
    def initialize(self, *a, **ka):
        self.api_defs_fpath = ka.pop("api_defs_fpath")
        self.api_defs_url = ka.pop("api_defs_url")
        super(SwaggerUIHandler, self).initialize(*a, **ka)
        self.api_defs_url_set = {"/%s" % self.api_defs_url, self.api_defs_url}
        self.default_filename_set = {"", "/", "/" + self.default_filename}

    def get(self, path, include_body=True):
        print("path", path)
        if path in self.default_filename_set:
            with open(join(self.root, self.default_filename)) as fd:
                buf = fd.read()
                self.write(buf % {
                    "api_defs_url": self.api_defs_url
                })
                self.flush()
            return
        elif path in self.api_defs_url_set:
            with open(self.api_defs_fpath) as fd:
                buf = fd.read()
                self.write(buf)
                self.flush()
            return
        return super(SwaggerUIHandler, self).get(path, include_body)

    @classmethod
    def add_at(cls, handlers, root_path, api_defs_fpath):
        if not isfile(api_defs_fpath):
            raise RuntimeError("Swagger 2.0 def file not found, or not a file: %s" % api_defs_fpath)
        assert root_path.startswith("/")
        assert len(root_path) >= 2
        if root_path.endswith("/"):
            assert len(root_path) > 2
            root_path = root_path[:-1]
        log.info("Starting swagger-ui API inspector at %s for %s" % (root_path, api_defs_fpath))
        ui_basepath = resource_filename("rap", "swagger-ui")
        api_defs_fname = basename(api_defs_fpath)
        handlers.append((r"%s[/]*(.*)" % root_path, cls,
                         {"path": ui_basepath,
                          "default_filename": "index.html",
                          "api_defs_url": api_defs_fname,
                          "api_defs_fpath": api_defs_fpath}))
