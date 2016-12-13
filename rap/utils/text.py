from six import string_types, binary_type, text_type
from urllib.request import quote


def astext(s):
    if isinstance(s, string_types):
        return s
    assert isinstance(s, binary_type)
    return text_type(s, "utf-8")


def uri_path_join(*args):
    res = ""
    trail = "/"
    for seg in args:
        if res:
            while seg and seg.startswith("/"):
                seg = seg[1:]
        while seg and seg.endswith("/"):
            seg = seg[:-1]
        trail = "/"
        if seg.endswith("^"):
            seg = seg[:-1]
            trail = ""
        if res:
            res += "/"
            seg = quote(seg)
        res += seg
    res += trail
    return res
