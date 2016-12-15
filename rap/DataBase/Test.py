import sqlite3 as lite
import sys
import uuid

DataBaseName = 'test.db'
TableName = 'Resources'

lite.register_converter('GUID', lambda b: uuid.UUID(bytes_le=b))
lite.register_adapter(uuid.UUID, lambda u: buffer(u.bytes_le))

def main():

    con = None

    try:
        con = lite.connect(DataBaseName)

        cur = con.cursor()
        cur.execute('SELECT SQLITE_VERSION()')

        data = cur.fetchone()

        print "SQLite version: %s" % data

    except lite.Error, e:

        print "Error %s:" % e.args[0]
        sys.exit(1)

    finally:

        if con:
            con.close()

def connect():
    conn = lite.connect(DataBaseName, detect_types=lite.PARSE_DECLTYPES)
    return conn

def inizializeTable():
    con = connect()

    with con:
        cur = con.cursor()
        cur.execute("DROP TABLE IF EXISTS "+TableName)
        cur.execute("CREATE TABLE Resources(GlobalId GUID PRIMARY KEY, PlatformId INT, Platform TEXT)")


def inizializeResources():
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

        cur.executemany("INSERT INTO " + TableName + " VALUES(?, ?, ?)", resources)
        # cur.execute("INSERT INTO "+TableName+" VALUES("+(uuid.uuid4())+",1,'Next')")

def addResource(platformId, platform,globalId=None):
    if (not globalId):
        globalId = uuid.uuid4()

    con = connect()
    with con:
        cur = con.cursor()

        cur.execute("INSERT INTO " + TableName + " VALUES(?, ?, ?)", (globalId,platformId,platform))


def getResources(globalId=None, platformId=None, platform=None):
    con = connect()

    with con:
        con.row_factory = lite.Row

        cur = con.cursor()
        cur.execute("SELECT * FROM " + TableName + " WHERE (GlobalId=? or ? is NULL) AND (PlatformId=? or ? is NULL) AND (Platform=? or ? is NULL)",
                    (globalId,globalId,platformId, platformId,platform,platform))

        rows = cur.fetchall()

        for row in rows:
            print "%s %i %s" %(row["GlobalId"], row["PlatformId"], row["Platform"])


def selectAll():
    con = connect()

    with con:
        con.row_factory = lite.Row

        cur = con.cursor()
        cur.execute("SELECT * FROM "+TableName)

        rows = cur.fetchall()

        for row in rows:
            print "%s %i %s" %(row["GlobalId"], row["PlatformId"], row["Platform"])


if __name__ == "__main__":
    main()
    #can use these two lines only first time
    inizializeTable()
    inizializeResources()


    selectAll()
    print("#### Get all resources of platform 'Works' ####")
    getResources(None,None,"Works");
    print("#### Add resource 77 of platform 'Works' ####")
    addResource(77,"Works");
    print("#### Get all resources of platform 'Works' ####")
    getResources(None, None, "Works");