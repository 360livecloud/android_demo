
package com.qihoo.videocloud.utils;

import com.qihoo.livecloud.tools.URLSafeBase64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by LeiXiaojun on 2017/9/20.
 */
public class AESUtil {

    private static final String algorithmStr = "AES/ECB/PKCS5Padding";

    private static byte[] encrypt(String content, String password) {
        try {
            byte[] keyStr = getKey(password);
            SecretKeySpec key = new SecretKeySpec(keyStr, "AES");
            Cipher cipher = Cipher.getInstance(algorithmStr);
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(byteContent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] decrypt(byte[] content, String password) {
        try {
            byte[] keyStr = getKey(password);
            SecretKeySpec key = new SecretKeySpec(keyStr, "AES");
            Cipher cipher = Cipher.getInstance(algorithmStr);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(content);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] getKey(String password) {
        byte[] rByte;
        if (password != null) {
            rByte = password.getBytes();
        } else {
            rByte = new byte[24];
        }
        return rByte;
    }

    /**
     *加密
     */
    public static String encode(String value, String privateKey) {
        return URLSafeBase64.encodeToString(encrypt(value, privateKey));
    }

    /**
     *解密
     */
    public static String decode(String value, String privateKey) {
        byte[] b = decrypt(URLSafeBase64.decode(value), privateKey);
        return new String(b);
    }
}
