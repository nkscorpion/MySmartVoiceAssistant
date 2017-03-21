package com.gizwits.opensource.appkit.utils;

import android.content.Context;

import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建者：TAN
 * 创建时间： 2017/3/17.
 */

public class MyUtil {
    /**
     * 读取asset目录下文件。
     *
     * @return content
     */
    public static String readFile(Context mContext, String file, String code) {
        int len = 0;
        byte[] buf = null;
        String result = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);
            
            result = new String(buf, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public static boolean check(String[] grammar, String mlc) {
        String str = Arrays.toString(grammar);
        Pattern rex = Pattern.compile(mlc);
        Matcher m = rex.matcher(str);
        return m.find();
    }
    
    public static void checkColor(String mlc) {
 
    }
    
    
    
}
