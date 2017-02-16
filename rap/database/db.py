import sqlite3 as lite
import uuid

import logging
log = logging.getLogger(__name__)

DATABASE_NAME = 'test.db'
RESOURCES_TABLE_NAME = 'Resources'
PLUGINS_TABLE_NAME = 'Plugins'

lite.register_converter('GUID', lambda b: uuid.UUID(bytes_le=b))
lite.register_adapter(uuid.UUID, lambda u: buffer(u.bytes_le))


def initialize_tables():
    con = connect()
    if con:
        cur = con.cursor()
        cur.execute("DROP TABLE IF EXISTS " + PLUGINS_TABLE_NAME)
        cur.execute(
            "CREATE TABLE " +
            PLUGINS_TABLE_NAME +
            "(PlatformId TEXT PRIMARY KEY, Description TEXT)")

        cur.execute("DROP TABLE IF EXISTS " + RESOURCES_TABLE_NAME)
        cur.execute(
            "CREATE TABLE " +
            RESOURCES_TABLE_NAME +
            "(GlobalId GUID PRIMARY KEY, PlatformResourceId INT, PlatformId TEXT)")
        con.commit()
    con.close()


#add fake resources for testing
def initialize_resources():
    con = connect()
    if con:
        cur = con.cursor()
        resources = (
            (uuid.UUID('85558919-e375-4ca8-9f5f-b617813c211d'), 1, 'Next'),
            (uuid.uuid4(), 2, 'Next'),
            (uuid.uuid4(), 3, 'Next'),
            (uuid.uuid4(), 1, 'Works'),
            (uuid.uuid4(), 2, 'Works')
        )
        cur.executemany("INSERT INTO " + RESOURCES_TABLE_NAME + " VALUES(?, ?, ?)", resources)
        # cur.execute("INSERT INTO "+TableName+" VALUES("+(uuid.uuid4())+",1,'Next')")
        con.commit()
    con.close()


def connect():
    conn = lite.connect(DATABASE_NAME, detect_types=lite.PARSE_DECLTYPES)
    return conn


def add_platform_plugin(platform_id, description=None):
    con = connect()
    if con:
        cur = con.cursor()

        cur.execute("SELECT * FROM " + PLUGINS_TABLE_NAME + " WHERE (PlatformId=?)", (platform_id,))
        rows = cur.fetchall()
        if rows.__len__() > 0:
            log.debug("This platform is already registered")
            con.close()
            return

        if not description:
            description = ""
        cur.execute("INSERT INTO " + PLUGINS_TABLE_NAME + " VALUES(?, ?)", (platform_id, description))
        con.commit()
        log.debug("Platform %s have been registered", platform_id)
        con.close()


def check_platform(platform_id):
    con = connect()
    if con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM " + PLUGINS_TABLE_NAME + " WHERE (PlatformId=?)"(platform_id,))
        rows = cur.fetchall()
        con.close()
        if rows.__len__() > 0:
            return True
    return False


def add_resource(global_id, platform_id, platform_resource_id):
    con = connect()
    if con:
        cur = con.cursor()

        cur.execute("SELECT * FROM " + RESOURCES_TABLE_NAME +
                    " WHERE (GlobalId=? or ? is NULL) AND "
                    "(PlatformResourceId=? or ? is NULL) AND "
                    "(PlatformId=? or ? is NULL)",
                    (global_id, global_id, platform_resource_id, platform_resource_id, platform_id, platform_id))
        rows = cur.fetchall()
        if rows.__len__() > 0:
            raise Exception("This resource is already registered")
        if not global_id:
            global_id = uuid.uuid4()
        cur.execute("INSERT INTO " + RESOURCES_TABLE_NAME + " VALUES(?, ?, ?)", (global_id, platform_resource_id, platform_id))
        con.commit()
        con.close()


def get_resources(global_id=None, platform_id=None, platform_resource_id=None):
    con = connect()
    if con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM " + RESOURCES_TABLE_NAME +
                    " WHERE (GlobalId=? or ? is NULL) AND "
                    "(PlatformResourceId=? or ? is NULL) AND "
                    "(PlatformId=? or ? is NULL)",
                    (global_id, global_id, platform_resource_id, platform_resource_id, platform_id, platform_id))
        rows = cur.fetchall()
        con.close()
        return rows


def update_resources(global_id, platform_id, platform_resource_id):
    change = False
    con = connect()
    if con:
        con.row_factory = lite.Row
        con.execute("UPDATE " + RESOURCES_TABLE_NAME +
                    " SET PlatformResourceId=?, PlatformId=?"
                    " WHERE GlobalId=?",
                    (platform_resource_id, platform_id, global_id))
        con.commit()

        change = con.total_changes > 0
        con.close()
    return change


def delete_resource(global_id):
    change = False
    con = connect()
    if con:
        con.row_factory = lite.Row
        con.execute("DELETE from " + RESOURCES_TABLE_NAME +
                    " WHERE GlobalId=?",
                    (global_id,))
        con.commit()

        change = con.total_changes > 0
        con.close()
    return change


def delete_resource_with_platform_id(platform_id):
    change = False
    con = connect()
    if con:
        con.row_factory = lite.Row
        con.execute("DELETE from " + RESOURCES_TABLE_NAME +
                    " WHERE PlatformId=?",
                    (platform_id,))
        con.commit()

        change = con.total_changes > 0
        con.close()
    return change


def get_resource(global_id):
    con = connect()
    if con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM " + RESOURCES_TABLE_NAME + " WHERE (GlobalId=?)"(global_id,))
        rows = cur.fetchall()
        platform_id = None
        platform_resource_id = None
        for row in rows:
            log.debug("%s %i %s" % (row["GlobalId"], row["PlatformResourceId"], row["PlatformId"]))
            platform_id = row["PlatformId"]
            platform_resource_id = row["PlatformResourceId"]
        con.close()

    return [platform_id, platform_resource_id]


def select_all():
    con = connect()
    if con:
        con.row_factory = lite.Row
        cur = con.cursor()
        cur.execute("SELECT * FROM "+ RESOURCES_TABLE_NAME)
        rows = cur.fetchall()
        for row in rows:
            log.debug("%s %i %s" % (row["GlobalId"], row["PlatformResourceId"], row["PlatformId"]))
        con.close()