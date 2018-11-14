
package com.qihoo.videocloud.interactbrocast.livingcammera;

import android.os.Environment;

import com.qihoo.faceapi.util.QhFaceInfo;
import com.qihoo.livecloud.tools.Logger;
import com.qihoo.videocloud.interactbrocast.faceu.DrawEff2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by liuyanqing on 2017/5/30.
 */

public class FaceUTest {

    private final String TAG = "FaceUTest";

    DrawEff2 m_draw_eff = new DrawEff2();

    DrawEff2.QhTracker m_qh_tracker = null;

    private int m_camera_preview_width = 720;
    private int m_camera_preview_height = 1280;
    private int m_n_face_detected_scale = 8;

    private boolean doRun = true;

    public FaceUTest() {
        doTest();
    }

    private void doTest() {

        final byte[] data_rotate_scale = read();

        if (m_qh_tracker == null) {
            m_qh_tracker = m_draw_eff.GetQhTracker();
        }

        if (data_rotate_scale == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (doRun) {
                    long time_beg = System.currentTimeMillis();

                    QhFaceInfo face = m_qh_tracker.DetectedFace(
                            data_rotate_scale, m_camera_preview_width / m_n_face_detected_scale,
                            m_camera_preview_height / m_n_face_detected_scale);
                    Logger.e(TAG, TAG + ", faceu time is: " + (System.currentTimeMillis() - time_beg));
                    if (face == null) {
                        Logger.e(TAG, TAG + ", face is null!");
                    } else {
                        Logger.e(TAG, TAG + ", get Face success!");
                    }

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

    }

    public byte[] read() {
        FileInputStream fin = null;
        try {

            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName;

            fin = new FileInputStream(path);

            //FileInputStream fin = openFileInput(fileName);

            //用这个就不行了，必须用FileInputStream

            int length = fin.available();

            byte[] buffer = new byte[length];

            fin.read(buffer);

            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fin = null;
            }
        }
        return null;
    }

    public static final String fileName = "faceuData.txt";

    /**
     * 根据byte数组生成文件
     *
     * @param bytes
     *            生成文件用到的byte数组
     */
    public static void createFileWithByte(byte[] bytes) {
        // TODO Auto-generated method stub
        /**
         * 创建File对象，其中包含文件所在的目录以及文件的命名
         */
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()) {
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(bytes);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

}
