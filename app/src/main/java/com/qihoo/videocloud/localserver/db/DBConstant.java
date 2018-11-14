
package com.qihoo.videocloud.localserver.db;

/**
 * Created by LeiXiaojun on 2017/9/29.
 */
public interface DBConstant {

    interface Download {

        String TABLE = "download";

        String COLUMN_ID = "id";
        String COLUMN_RID = "rid";
        String COLUMN_URL = "url";
        String COLUMN_FILE = "file";
        String COLUMN_STATE = "state";
        String COLUMN_POSITION = "position";
        String COLUMN_TOTAL = "total";
        String COLUMN_ERROR_CODE = "err_code";
        String COLUMN_ERROR_MESSAGE = "err_msg";

        String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS download (\n" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  rid TEXT NOT NULL,\n" +
                "  url TEXT NOT NULL,\n" +
                "  file TEXT,\n" +
                "  state NOT NULL,\n" +
                "  position LONG,\n" +
                "  total LONG,\n" +
                "  err_code INTEGER,\n" +
                "  err_msg TEXT\n" +
                ")\n";

        String SQL_DROP_TABLE = "DROP TABLE download";
    }
}
