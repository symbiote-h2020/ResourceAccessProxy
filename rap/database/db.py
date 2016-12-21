import sqlite3 as lite
import sys
import uuid

import logging
log = logging.getLogger(__name__)

DATABASE_NAME = 'test.db'
TABLE_NAME = 'Resources'

lite.register_converter('GUID', lambda b: uuid.UUID(bytes_le=b))
lite.register_adapter(uuid.UUID, lambda u: buffer(u.bytes_le))


def initialize_table():
    con = connect()
    with con:
        cur = con.cursor()
        cur.execute("DROP TABLE IF EXISTS " + TABLE_NAME)
        cur.execute("CREATE TABLE Resources(GlobalId GUID PRIMARY KEY, PlatformResourceId INT, PlatformId TEXT)")


def initialize_resources():
    con = connect()
    with con:
        cur = con.cursor()
        resources = (
            (uuid.UUID('85558919-e375-4ca8-9f5f-b617813c211d'), 1, 'Next'),
            (uuid.uuid4(), 2, 'Next'),
            (uuid.uuid4(), 3, 'Next'),
            (uuid.uuid4(), 1, 'Works'),
            (uuid.uuid4(), 2, 'Works')
        )
        cur.executemany("INSERT INTO " + TABLE_NAME + " VALUES(?, ?, ?)", resources)
        # cur.execute("INSERT INTO "+TableName+" VALUES("+(uuid.uuid4())+",1,'Next')")


def connect_to_db():
    con = None
    try:
        con = lite.connect(DATABASE_NAME)
        cur = con.cursor()
        cur.execute('SELECT SQLITE_VERSION()')
        data = cur.fetchone()
        log.debug("SQLite version: %s", data);
    except lite.Error, e:
        print "Error %s:" % e.args[0]
        sys.exit(1)
    finally:
        if con:
            con.close()


def connect():
    conn = lite.connect(DATABASE_NAME, detect_types=lite.PARSE_DECLTYPES)
    return conn


def add_resource(platform_resource_id, platform_id, global_id=None):
    if not global_id:
        global_id = uuid.uuid4()
    con = connect()
    with con:
        cur = con.cursor()
        cur.execute("INSERT INTO " + TABLE_NAME + " VALUES(?, ?, ?)", (global_id, platform_resource_id, platform_id))


def get_resources(global_id=None, platform_resource_id=None, platform_id=None):
    con = connect()
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM " + TABLE_NAME +
                    " WHERE (GlobalId=? or ? is NULL) AND "
                    "(PlatformResourceId=? or ? is NULL) AND "
                    "(PlatformId=? or ? is NULL)",
                    (global_id, global_id, platform_resource_id, platform_resource_id, platform_id, platform_id))
        rows = cur.fetchall()
        for row in rows:
            log.debug("%s %i %s" % (row["GlobalId"], row["PlatformResourceId"], row["PlatformId"]))


def get_resource(global_id=None):
    con = connect()
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM " + TABLE_NAME + " WHERE (GlobalId=?)"(global_id))
        rows = cur.fetchall()
        platform_id = None
        platform_resource_id = None
        for row in rows:
            log.debug("%s %i %s" % (row["GlobalId"], row["PlatformResourceId"], row["PlatformId"]))
            platform_id = row["PlatformId"]
            platform_resource_id = row["PlatformResourceId"]

    return [platform_id, platform_resource_id]


def select_all():
    con = connect()
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM "+ TABLE_NAME)
        rows = cur.fetchall()
        for row in rows:
            log.debug("%s %i %s" % (row["GlobalId"], row["PlatformResourceId"], row["PlatformId"]))