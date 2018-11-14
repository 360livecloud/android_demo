
package com.qihoo.videocloud.utils;

import org.xutils.x;

public class LibTaskController {

    /**
     * 在UI线程执行runnable.
     * 如果已在UI线程, 则直接执行.
     *
     * @param runnable
     */
    public static void autoPost(Runnable runnable) {
        x.task().autoPost(runnable);
    }

    /**
     * 在UI线程执行runnable.
     * post到msg queue.
     *
     * @param runnable
     */
    public static void post(Runnable runnable) {
        x.task().post(runnable);
    }

    /**
     * 在UI线程执行runnable.
     *
     * @param runnable
     * @param delayMillis 延迟时间(单位毫秒)
     */
    public static void postDelayed(Runnable runnable, long delayMillis) {
        x.task().postDelayed(runnable, delayMillis);
    }

    /**
     * 在后台线程执行runnable
     *
     * @param runnable
     */
    public static void run(Runnable runnable) {
        x.task().run(runnable);
    }

    /**
     * 移除post或postDelayed提交的, 未执行的runnable
     *
     * @param runnable
     */
    public static void removeCallbacks(Runnable runnable) {
        x.task().removeCallbacks(runnable);
    }

}
