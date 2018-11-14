
package com.qihoo.videocloud.upload;

import android.text.TextUtils;

import com.qihoo.livecloud.tools.MD5;
import com.qihoo.livecloud.upload.QHVCKeyGenerator;

import java.io.File;

/**
 * Created by guohailiang on 11/23/2017.
 */

public class FileKeyGenerator implements QHVCKeyGenerator {

    @Override
    public String gen(File file) {
        if (file != null && file.exists() && file.isFile()) {
            String md5 = MD5.encryptMD5(file);
            if (!TextUtils.isEmpty(md5)) {
                return md5;
            } else {
                return MD5.encryptMD5(file.getAbsolutePath());
            }
        } else {
            return "";
        }
    }
}
