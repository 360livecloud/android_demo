
package com.qihoo.videocloud;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.StrictMode;

import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.qihoo.livecloud.sdk.DebugUtils;
import com.qihoo.livecloud.sdk.QHVCSdk;
import com.qihoo.livecloud.sdk.QHVCSdkConfig;
import com.qihoo.livecloud.sdk.QHVCServerAddress;
import com.qihoo.livecloud.tools.Constants;
import com.qihoo.livecloud.tools.DeviceIDUtils;
import com.qihoo.livecloud.tools.Stats;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.beauty.BeautyHelper;
import com.qihoo.videocloud.debug.Setting;
import com.qihoo.videocloud.utils.Utils;
import com.tencent.bugly.crashreport.CrashReport;

import org.xutils.x;

public class VideoCloudApplication extends Application {
    private static final boolean DEBUG = true;

    private static final String TAG = VideoCloudApplication.class.getSimpleName();

    private static VideoCloudApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        useStrictMode();
        x.Ext.init(this);
        x.Ext.setDebug(true); // 是否输出debug日志, 开启debug会影响性能.
        buglyInit();
        imageLoaderInit();

        QHVCSdkInit();
    }

    public static VideoCloudApplication getInstance() {
        return instance;
    }

    private void QHVCSdkInit() {

        // 调试信息
        DebugUtils debugUtils = new DebugUtils();
        debugUtils.setWriteLogs(true)
                .setPlayerLogLevel(Constants.ELogLevel.LOG_LEVEL_FATAL)
                .setTransportLogLevel(Constants.ELogLevel.LOG_LEVEL_TRACE);

        // 直播云SDK配置
        QHVCSdkConfig.Builder builder = new QHVCSdkConfig.Builder(this)
                .setBusinessId(getResources().getString(R.string.config_bid))
                .setAppVersion("0.0")
                .setMachineId(DeviceIDUtils.getIMEI2(this))
                .setUserId(getResources().getString(R.string.config_uid))//可选项
                .setDebugUtils(debugUtils);

        /**
         * [test]服务端地址配置. sdk接入时，不需要关注！
         */
        QHVCServerAddress serverAddress = Setting.readServerAddress();
        if (serverAddress != null) {
            builder.setServerAddress(serverAddress);
        }

        QHVCSdk.getInstance().init(builder.build());

        /**
         * [test]设置打点地址. sdk接入时，不需要关注！
         */
        setStatsURL();

        BeautyHelper.initFaceUAndBeauty(this);/*美颜初始化和FaceU鉴权*/
    }

    /**
     * 设置打点地址
     */
    private static void setStatsURL() {
        QHVCSdkConfig qhvcSdkConfig = QHVCSdk.getInstance().getConfig();
        Stats.test_setNotifyUrl(qhvcSdkConfig.getStatUrl(), qhvcSdkConfig.getFeedbackUrl(), qhvcSdkConfig.getMicUrl(), qhvcSdkConfig.getControlUrl());
    }

    public void applicationExit() {
        onTerminate();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void useStrictMode() {
        if (DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    //                    .penaltyDeath()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    //                    .penaltyDeath()
                    .penaltyLog()
                    .build());
        }
    }

    private void buglyInit() {
        String packageName = this.getPackageName();
        String processName = Utils.getProcessName(android.os.Process.myPid());

        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        strategy.setAppChannel("home");
        strategy.setAppVersion(Utils.getVersionName(this));
        strategy.setAppPackageName(getPackageName());

        CrashReport.initCrashReport(this, "e3f325ec53", true, strategy);
    }

    private void imageLoaderInit() {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT).build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .threadPriority(Thread.NORM_PRIORITY - 1)
                .threadPoolSize(3)
                .memoryCacheSize(4 * 1024 * 1024)
                .memoryCache(new UsingFreqLimitedMemoryCache(4 * 1024 * 1024))
                .diskCacheSize(128 * 1024 * 1024)
                .diskCacheFileCount(100)
                .imageDownloader(new BaseImageDownloader(this, 30 * 1000, 30 * 1000))
                .defaultDisplayImageOptions(options) // default
                .writeDebugLogs()
                .tasksProcessingOrder(QueueProcessingType.FIFO).build();
        ImageLoader.getInstance().init(config);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        QHVCSdk.getInstance().destroy();
    }
}
