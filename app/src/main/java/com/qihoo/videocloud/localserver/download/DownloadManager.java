
package com.qihoo.videocloud.localserver.download;

import android.content.ContentValues;
import android.util.Log;

import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.localserver.db.DB;
import com.qihoo.videocloud.localserver.db.DBConstant;
import com.qihoo.videocloud.localserver.setting.LocalServerSettingConfig;

import net.qihoo.videocloud.LocalServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by LeiXiaojun on 2017/9/27.
 */
public class DownloadManager {

    private static DownloadManager sInst = null;

    private DB.DownloadTable mDownloadTable;

    private List<DownloadTask> mDownloadTaskList;
    private HashMap<String, DownloadTask> mDownloadTaskMap;

    protected DownloadManager() {
        mDownloadTable = DB.getInstance().getDownloadTable();

        mDownloadTaskList = new LinkedList<>();
        mDownloadTaskMap = new HashMap<>();
    }

    public static synchronized DownloadManager getInstance() {
        if (sInst == null) {
            sInst = new DownloadManager();
        }
        return sInst;
    }

    public void init() {
        Logger.i(LocalServerSettingConfig.TAG, "DownloadManager init");
        loadData();
        LocalServer.setCachePersistenceCallback(mCachePersistenceCallback);
    }

    public void unInit() {
        Logger.i(LocalServerSettingConfig.TAG, "DownloadManager unInit");
        unloadData();
        LocalServer.setCachePersistenceCallback(null);
    }

    private void loadData() {
        mDownloadTaskList.clear();
        mDownloadTaskMap.clear();

        List<DownloadTask> downloadTasks = mDownloadTable.select();
        if (downloadTasks != null) {
            for (DownloadTask downloadTask : downloadTasks) {
                //首次加载，需要将上一次正在下载的任务状态重置为暂停
                if (downloadTask.state == DownloadConstant.State.STATE_DOWNLOADING) {
                    downloadTask.state = DownloadConstant.State.STATE_PAUSED;

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
                    mDownloadTable.update(downloadTask.rid, contentValues);
                }

                //首次加载，需要在LocalServer中重建下载任务
                LocalServer.rebuildPersistence(downloadTask.rid, downloadTask.url, downloadTask.file);

                //任务已完成数据量不会存储到数据库，需要主动查询获取
                LocalServer.CachedSize cachedSize = new LocalServer.CachedSize();
                if (LocalServer.getCachePersistenceSize(downloadTask.rid, cachedSize)) {
                    downloadTask.position = cachedSize.cachedSize;
                }

                Log.i(LocalServerSettingConfig.TAG, "loadPersistence " + downloadTask.toString());

                mDownloadTaskList.add(downloadTask);
                mDownloadTaskMap.put(downloadTask.rid, downloadTask);
            }

            notifyDataObserver();
        }
    }

    private void unloadData() {
        mDownloadTaskList.clear();
        mDownloadTaskMap.clear();

        notifyDataObserver();
    }

    interface DataObserver {

        void onDataObserver();
    }

    private HashSet<DataObserver> mDataObservers = new HashSet<>();

    public void addDataObserver(DataObserver dataObserver) {
        mDataObservers.add(dataObserver);
    }

    public void removeDataObserver(DataObserver dataObserver) {
        mDataObservers.remove(dataObserver);
    }

    public void notifyDataObserver() {
        for (DataObserver dataObserver : mDataObservers) {
            dataObserver.onDataObserver();
        }
    }

    public List<DownloadTask> getTaskList() {
        return mDownloadTaskList;
    }

    public DownloadTask getTask(String rid) {
        if (mDownloadTaskMap.containsKey(rid)) {
            return mDownloadTaskMap.get(rid);
        }
        return null;
    }

    public boolean cachePersistence(DownloadTask downloadTask) {
        if (downloadTask == null) {
            return false;
        }

        Log.i(LocalServerSettingConfig.TAG, "cachePersistence rid=" + downloadTask.rid +
                ", url=" + downloadTask.url +
                ", file=" + downloadTask.file +
                ", state=" + downloadTask.state);

        boolean exist = mDownloadTaskMap.containsKey(downloadTask.rid);

        boolean ret = LocalServer.cachePersistence(downloadTask.rid, downloadTask.url, downloadTask.file);
        if (ret && !exist) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBConstant.Download.COLUMN_RID, downloadTask.rid);
            contentValues.put(DBConstant.Download.COLUMN_URL, downloadTask.url);
            contentValues.put(DBConstant.Download.COLUMN_FILE, downloadTask.file);
            contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
            mDownloadTable.insert(contentValues);

            mDownloadTaskList.add(0, downloadTask);
            mDownloadTaskMap.put(downloadTask.rid, downloadTask);

            notifyDataObserver();
        }

        return ret;
    }

    public boolean resumeCachePersistence(DownloadTask downloadTask) {
        if (downloadTask == null) {
            return false;
        }

        Log.i(LocalServerSettingConfig.TAG, "resumeCachePersistence rid=" + downloadTask.rid + ", state=" + downloadTask.state);

        if (!mDownloadTaskMap.containsKey(downloadTask.rid)) {
            return false;
        }

        boolean ret = LocalServer.resumeCachePersistence(downloadTask.rid);
        if (ret) {
            downloadTask.state = DownloadConstant.State.STATE_DOWNLOADING;

            ContentValues contentValues = new ContentValues();
            contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
            mDownloadTable.update(downloadTask.rid, contentValues);

            notifyDataObserver();
        }

        return ret;
    }

    public boolean pauseCachePersistence(DownloadTask downloadTask) {
        if (downloadTask == null) {
            return false;
        }

        Log.i(LocalServerSettingConfig.TAG, "pauseCachePersistence rid=" + downloadTask.rid + ", state=" + downloadTask.state);

        if (!mDownloadTaskMap.containsKey(downloadTask.rid)) {
            return false;
        }

        boolean ret = LocalServer.pauseCachePersistence(downloadTask.rid);
        if (ret) {
            downloadTask.state = DownloadConstant.State.STATE_PAUSED;

            ContentValues contentValues = new ContentValues();
            contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
            mDownloadTable.update(downloadTask.rid, contentValues);

            notifyDataObserver();
        }

        return ret;
    }

    public boolean cancelCachePersistence(DownloadTask downloadTask, boolean deleteFile) {
        if (downloadTask == null) {
            return false;
        }

        Log.i(LocalServerSettingConfig.TAG, "cancelCachePersistence rid=" + downloadTask.rid + ", state=" + downloadTask.state);

        if (!mDownloadTaskMap.containsKey(downloadTask.rid)) {
            return false;
        }

        boolean ret = LocalServer.cancelCachePersistence(downloadTask.rid, deleteFile);
        if (ret) {
            mDownloadTable.delete(downloadTask.rid);

            mDownloadTaskList.remove(downloadTask);
            mDownloadTaskMap.remove(downloadTask.rid);

            notifyDataObserver();
        }

        return ret;
    }

    private LocalServer.CachePersistenceCallback mCachePersistenceCallback = new LocalServer.CachePersistenceCallback() {

        @Override
        public void onStart(String rid) {
            Log.i(LocalServerSettingConfig.TAG, "CachePersistenceCallback onStart rid=" + rid);

            DownloadTask downloadTask = getTask(rid);
            if (downloadTask != null) {
                downloadTask.state = DownloadConstant.State.STATE_DOWNLOADING;

                ContentValues contentValues = new ContentValues();
                contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
                contentValues.put(DBConstant.Download.COLUMN_ERROR_CODE, 0);
                contentValues.put(DBConstant.Download.COLUMN_ERROR_MESSAGE, "");
                mDownloadTable.update(downloadTask.rid, contentValues);

                notifyDataObserver();
            }
        }

        @Override
        public void onProgress(String rid, long position, long total, double speed) {
            Log.i(LocalServerSettingConfig.TAG, "CachePersistenceCallback onProgress=" + rid +
                    ", position=" + position + ", total=" + total + ", speed=" + speed);

            DownloadTask downloadTask = getTask(rid);
            if (downloadTask != null) {
                //首次获取到任务总大小，记录到数据库
                if (downloadTask.total == 0) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DBConstant.Download.COLUMN_TOTAL, total);
                    mDownloadTable.update(downloadTask.rid, contentValues);
                }

                downloadTask.position = position;
                downloadTask.total = total;
                downloadTask.speed = speed;

                notifyDataObserver();
            }
        }

        @Override
        public void onSuccess(String rid) {
            Log.i(LocalServerSettingConfig.TAG, "CachePersistenceCallback onSuccess rid=" + rid);

            DownloadTask downloadTask = getTask(rid);
            if (downloadTask != null) {
                downloadTask.state = DownloadConstant.State.STATE_DOWNLOADED;

                ContentValues contentValues = new ContentValues();
                contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
                mDownloadTable.update(downloadTask.rid, contentValues);

                notifyDataObserver();
            }
        }

        @Override
        public void onFailed(String rid, int errCode, String errMsg) {
            Log.i(LocalServerSettingConfig.TAG, "CachePersistenceCallback onFailed rid=" + rid +
                    ", errCode=" + errCode + ", errMsg=" + errMsg);

            DownloadTask downloadTask = getTask(rid);
            if (downloadTask != null) {
                downloadTask.state = DownloadConstant.State.STATE_FAILED;
                downloadTask.errCode = errCode;
                downloadTask.errMsg = errMsg;

                ContentValues contentValues = new ContentValues();
                contentValues.put(DBConstant.Download.COLUMN_STATE, downloadTask.state);
                contentValues.put(DBConstant.Download.COLUMN_ERROR_CODE, downloadTask.errCode);
                contentValues.put(DBConstant.Download.COLUMN_ERROR_MESSAGE, downloadTask.errMsg);
                mDownloadTable.update(downloadTask.rid, contentValues);

                notifyDataObserver();
            }
        }
    };
}
