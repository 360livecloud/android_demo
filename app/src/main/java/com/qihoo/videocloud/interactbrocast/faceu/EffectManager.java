package com.qihoo.videocloud.interactbrocast.faceu;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.JsonReader;

import com.qihoo.livecloud.tools.Logger;
import com.qihoo.livecloud.utils.DiskUtils;
import com.qihoo.livecloud.utils.FileUtils;
import com.qihoo.videocloud.VideoCloudApplication;
import com.qihoo.videocloud.utils.QHVCSharedPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//import com.engine.imageloader.FrescoImageLoader;
//import com.facebook.common.references.CloseableReference;
//import com.facebook.datasource.DataSource;
//import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
//import com.facebook.imagepipeline.image.CloseableImage;
//import com.huajiao.GlobalFunctions;
//import com.huajiao.base.BaseApplication;


public class EffectManager {
    private static final String TAG = "EffectManager";

    public static final String QH_FACE_MODEL_FOLDER_NAME = "model";
    private static final String PREFERENCE_MODEL = "faceu_model";
    private static final String PATH = "LiveCloud";

    private String _effectName;
    private String _effectID;
    private int _type;
    private int _loop;
    private String _music;

    private AssetManager assetManager;
    List<TextureFeature> textureList = null;
    boolean m_b_use_assets = true;


    public EffectManager() {
        textureList = new ArrayList<>();
    }

    private int parseJson(String str_json) {
        //step 2 parset the config string
        JsonReader jsonReader = new JsonReader(new StringReader(str_json));

        try {
            jsonReader.beginObject();
            jsonReader.nextName();
            _effectName = jsonReader.nextString();
            jsonReader.nextName();
            _effectID = jsonReader.nextString();
            jsonReader.nextName();
            _type = jsonReader.nextInt();
            jsonReader.nextName();
            _loop = jsonReader.nextInt();
            jsonReader.nextName();
            _music = jsonReader.nextString();

            if (jsonReader.nextName().equals("texture")) {
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    TextureFeature textureFeature = new TextureFeature();
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        switch (jsonReader.nextName()) {
                            case "mframeCount":
                                textureFeature._frameCount = jsonReader.nextInt();
                                break;
                            case "radius_Type":
                                textureFeature._radiusType = jsonReader.nextInt();
                                break;
                            case "mradius":
                                textureFeature._radius = jsonReader.nextInt();
                                break;
                            case "mid_Type":
                                textureFeature._midType = jsonReader.nextInt();
                                break;
                            case "scale_Type":
                                textureFeature._scaleType = jsonReader.nextInt();
                                break;
                            case "scale_ratio":
                                textureFeature._scaleRatio = Float.parseFloat(jsonReader.nextString());
                                break;
                            case "anchor_offset_x":
                                textureFeature._x = jsonReader.nextInt();
                                break;
                            case "anchor_offset_y":
                                textureFeature._y = jsonReader.nextInt();
                                break;
                            case "asize_offset_x":
                                textureFeature._w = jsonReader.nextInt();
                                break;
                            case "asize_offset_y":
                                textureFeature._h = jsonReader.nextInt();
                                break;
                            case "mfaceCount":
                                textureFeature._faceCount = jsonReader.nextInt();
                                break;
                            case "imageName":
                                textureFeature._folderName = jsonReader.nextString();
                                break;
                            case "mid_x":
                                textureFeature._midX = Float.parseFloat(jsonReader.nextString());
                                break;
                            case "mid_y":
                                textureFeature._midY = Float.parseFloat(jsonReader.nextString());
                                break;
                            default:
                                return -2;
                        }

                    }
                    jsonReader.endObject();
                    textureList.add(textureFeature);
                }
                jsonReader.endArray();
                jsonReader.endObject();
                jsonReader.close();
            } else {
                return -3;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -4;
        }
        return 0;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public int parseAssetsConfig(Context context, String effectID) {
        m_b_use_assets = true;
        String folderName = "eff/" + effectID + "/com/qihoo/livecloud/config";
        assetManager = context.getAssets();
        String jsonData = "";
        //step 1: read the config string
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(assetManager.open(folderName));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                jsonData += line;
            }
            bufferedReader.close();
            inputStreamReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        return parseJson(jsonData);
    }

    public int parseLocalConfig(String effectID) {
        m_b_use_assets = false;
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "faceeff";
        String str_folder = path + File.separator;
        String folderName = str_folder + effectID + "/com/qihoo/livecloud/config";
        String jsonData = "";
        //step 1: read the config string
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(folderName));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                jsonData += line;
            }
            bufferedReader.close();
            inputStreamReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        return parseJson(jsonData);
    }

    public int GetPngTotalNum() {
        int n_total = 0;
        for (int i = 0; i < getTextureNum(); i++) {
            n_total += getTextureFrameCount(i);
        }
        return n_total;
    }

    public int getTextureNum() {
        return textureList.size();
    }

    public int getTextureX(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._x;
    }

    public int getTextureY(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._y;
    }

    public int getTextureW(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._w;
    }

    public int getTextureH(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._h;
    }

    public int getTextureRadiusType(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._radiusType;
    }

    public int getTextureRadius(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._radius;
    }

    public int getTextureMidType(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._midType;
    }

    public int getTextureScaleType(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._scaleType;
    }

    public float getTextureScaleRation(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._scaleRatio;
    }

    public int getTextureFaceCount(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._faceCount;
    }

    public int getTextureFrameCount(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._frameCount;
    }

    public String getTextureFolderName(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._folderName;
    }

    public float getTextureMidX(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._midX;
    }

    public float getTextureMidY(int index) {
        assert (textureList.size() != 0);
        if (index >= textureList.size()) {
            index = index % textureList.size();
        }
        return textureList.get(index)._midY;
    }

    public String GetPngName(int indexOfFolder, int indexOfImage) {
        TextureFeature textureFeature = textureList.get(indexOfFolder);
        return textureFeature._folderName + indexOfImage + ".png";
    }

    public Bitmap GetBitmap(int indexOfFolder, int indexOfImage) {
        if (m_b_use_assets) {
            return getBitmapFromAssets(indexOfFolder, indexOfImage);
        } else {
            return getBitmapFromLocal(indexOfFolder, indexOfImage);
            //return getBitmapFromLocalUseFresco(indexOfFolder, indexOfImage);
        }
    }

    private String GetPngAssetName(int indexOfFolder, int indexOfImage) {
        TextureFeature textureFeature = textureList.get(indexOfFolder);
        String currentImageName = "eff/" + _effectID + "/" + textureFeature._folderName + "/" + textureFeature._folderName + indexOfImage + ".png";
        return currentImageName;
    }

    private String GetPngLocalName(int indexOfFolder, int indexOfImage) {
        TextureFeature textureFeature = textureList.get(indexOfFolder);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "faceeff";
        String currentImageName = path + File.separator + _effectID + "/" + textureFeature._folderName + "/" + textureFeature._folderName + indexOfImage + ".png";
        return currentImageName;
    }

    private Bitmap getBitmapFromAssets(int indexOfFolder, int indexOfImage) {
        String currentImageName = GetPngAssetName(indexOfFolder, indexOfImage);
        InputStream in = null;
        try {
            in = assetManager.open(currentImageName);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Throwable e) {

        }
        return bitmap;
    }

    private Bitmap getBitmapFromLocal(int indexOfFolder, int indexOfImage) {
        String currentImageName = GetPngLocalName(indexOfFolder, indexOfImage);
        InputStream in = null;
        try {
            in = new FileInputStream(currentImageName);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Throwable e) {

        }
        return bitmap;
    }

    /**
     * 拷贝美颜需要的model文件到SD卡
     */
    public static void copyAndUnzipModelFiles() {
        if (QHVCSharedPreferences.getInstence().getBoolean(PREFERENCE_MODEL, false) == true) {
            return;
        }
        String str_path = getAppDir(VideoCloudApplication.getInstance()) + QH_FACE_MODEL_FOLDER_NAME;
        File folder_file = new File(str_path);

        if (!folder_file.isDirectory() || folder_file.listFiles() == null || folder_file.listFiles().length <= 0) {
            FileUtils.deleteFile(str_path);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String file_name = QH_FACE_MODEL_FOLDER_NAME + ".zip";
                    String file_out = getAppDir(VideoCloudApplication.getInstance()) + file_name;
                    AssetManager assetManager = VideoCloudApplication.getInstance().getAssets();
                    int byteread = 0;
                    try {
                        InputStream is = assetManager.open(file_name);
                        FileOutputStream fs = new FileOutputStream(file_out);
                        byte[] buffer = new byte[2048];

                        while ((byteread = is.read(buffer)) != -1) {
                            fs.write(buffer, 0, byteread);
                        }

                        is.close();
                        fs.close();
                    } catch (Throwable e) {
                        return;
                    }

                    UnZipFolder(file_out, getAppDir(VideoCloudApplication.getInstance()));
                    FileUtils.deleteFile(file_out);
                    QHVCSharedPreferences.getInstence().putBooleanValue(PREFERENCE_MODEL, true);
                    if (Logger.LOG_ENABLE) {
                        Logger.d(TAG, TAG + "，model.zip copy and unzip success!!");
                    }
                }
            }).start();
        } else {
            return;
        }
    }

    public static String getAppDir(Context context) {
        String dir = "";
        if (DiskUtils.checkSDCard()) {
            dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            dir = context.getDir("livecloud", Context.MODE_PRIVATE).getAbsolutePath();
        }
        dir = dir + File.separator + "LiveCloud" + File.separator;
        FileUtils.createDir(dir);

        String str_Hide_FilePath = dir + ".nomedia";
        File fHide = new File(str_Hide_FilePath);
        if (!fHide.isFile()) {
            try {
                fHide.createNewFile();
            } catch (Exception e) {
            }

        }
        return dir;
    }

    private static boolean UnZipFolder(String zipFileString, String outPathString) {
        boolean b_ret = true;
        try {
            ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
            ZipEntry zipEntry;
            String szName = "";
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    // get the folder name of the widget
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPathString + File.separator + szName);
                    folder.mkdirs();
                } else {
                    int n_last_index = szName.lastIndexOf("/");
                    if (n_last_index != -1) {
                        String str_folder_name = outPathString + File.separator + szName.substring(0, n_last_index);
                        File folder = new File(str_folder_name);
                        if (!folder.isDirectory()) {
                            folder.mkdirs();
                        }
                    }
                    File file = new File(outPathString + File.separator + szName);
                    file.createNewFile();
                    // get the output stream of the file
                    FileOutputStream out = new FileOutputStream(file);
                    int len;
                    byte[] buffer = new byte[1024];
                    // read (len) bytes into buffer
                    while ((len = inZip.read(buffer)) != -1) {
                        // write (len) byte from buffer at the position 0
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                    out.close();
                }
            }
            inZip.close();
        } catch (Throwable e) {
            b_ret = false;
        }
        return b_ret;
    }

//    Bitmap m_bm_tmp;
//    AtomicInteger m_n_load_bm = new AtomicInteger(0);
//    private Bitmap getBitmapFromLocalUseFresco(int indexOfFolder,int indexOfImage)
//    {
//        long n_beg = System.currentTimeMillis();
//        m_bm_tmp = null;
//        m_n_load_bm.set(0);
//        String currentImageName = GetPngLocalName(indexOfFolder, indexOfImage);
//        currentImageName = "file://" + currentImageName;
//        FrescoImageLoader.getInstance().loadBitmap(currentImageName, BaseApplication.getContext(), new BaseBitmapDataSubscriber()
//        {
//            @Override
//            protected void onNewResultImpl(Bitmap bitmap)
//            {
//                m_bm_tmp = bitmap;
//                m_n_load_bm.set(1);
//
//            }
//
//            @Override
//            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource)
//            {
//                m_bm_tmp = null;
//                m_n_load_bm.set(2);
//            }
//        });
//
//        while (m_n_load_bm.get() == 0)
//        {
//            if ((System.currentTimeMillis() - n_beg) >= 100)
//            {
//                break;
//            }
//        }
//
//        return m_bm_tmp;
//    }

    public void clear() {
        if (textureList != null) {
            textureList.clear();
        }
    }

    private class TextureFeature {
        public float _midY;
        public float _midX;
        private int _x;
        private int _y;
        private int _w;
        private int _h;
        private int _radiusType;
        private int _radius;
        private int _midType;
        private int _scaleType;
        private float _scaleRatio;
        private int _faceCount;
        private int _frameCount;
        private String _folderName;
    }

}
