
package com.qihoo.videocloud.localserver.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.qihoo.videocloud.VideoCloudApplication;
import com.qihoo.videocloud.localserver.download.DownloadTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/9/29.
 */
public class DB {

    private DBOpenHelper mDBOpenHelper;

    private DownloadTable mDownloadTable;

    private static DB mDB = null;

    protected DB(Context context) {
        mDBOpenHelper = new DBOpenHelper(context, "localserver", 1);
    }

    public static synchronized DB getInstance() {
        if (mDB == null) {
            mDB = new DB(VideoCloudApplication.getInstance());
        }
        return mDB;
    }

    public synchronized DownloadTable getDownloadTable() {
        if (mDownloadTable == null) {
            mDownloadTable = new DownloadTable();
        }
        return mDownloadTable;
    }

    public class DownloadTable {

        public List<DownloadTask> select() {
            List<DownloadTask> downloadTasks = new ArrayList<>();

            Cursor cursor = null;
            try {
                cursor = mDBOpenHelper.getReadableDatabase().query(DBConstant.Download.TABLE, null, null, null, null, null, "id DESC");

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        DownloadTask downloadTask = parseCursor(cursor);
                        if (downloadTask != null) {
                            downloadTasks.add(downloadTask);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return downloadTasks;
        }

        public DownloadTask select(String rid) {
            Cursor cursor = null;
            try {
                cursor = mDBOpenHelper.getReadableDatabase().query(DBConstant.Download.TABLE, null, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToNext();
                    return parseCursor(cursor);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return null;
        }

        private DownloadTask parseCursor(Cursor cursor) {
            if (cursor == null) {
                return null;
            }

            DownloadTask downloadTask = new DownloadTask();
            downloadTask.rid = cursor.getString(cursor.getColumnIndex(DBConstant.Download.COLUMN_RID));
            downloadTask.url = cursor.getString(cursor.getColumnIndex(DBConstant.Download.COLUMN_URL));
            downloadTask.file = cursor.getString(cursor.getColumnIndex(DBConstant.Download.COLUMN_FILE));
            downloadTask.state = cursor.getInt(cursor.getColumnIndex(DBConstant.Download.COLUMN_STATE));
            downloadTask.position = cursor.getLong(cursor.getColumnIndex(DBConstant.Download.COLUMN_POSITION));
            downloadTask.total = cursor.getLong(cursor.getColumnIndex(DBConstant.Download.COLUMN_TOTAL));
            downloadTask.errCode = cursor.getInt(cursor.getColumnIndex(DBConstant.Download.COLUMN_ERROR_CODE));
            downloadTask.errMsg = cursor.getString(cursor.getColumnIndex(DBConstant.Download.COLUMN_ERROR_MESSAGE));

            return downloadTask;
        }

        public boolean insert(ContentValues contentValues) {
            long ret = mDBOpenHelper.getWritableDatabase().insert(DBConstant.Download.TABLE, null, contentValues);
            return ret >= 0;
        }

        public boolean delete(String rid) {
            return mDBOpenHelper.getWritableDatabase().delete(DBConstant.Download.TABLE, "rid=?", new String[] {
                    rid
            }) > 0;
        }

        public boolean update(String rid, ContentValues contentValues) {
            return mDBOpenHelper.getWritableDatabase().update(DBConstant.Download.TABLE, contentValues, "rid=?", new String[] {
                    rid
            }) > 0;
        }
    }
}
