package com.kii.kiigatekeeper;

import android.content.Context;
import android.content.SharedPreferences;

import com.kii.cloud.storage.KiiUser;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yue on 14/12/1.
 */
public class Utils {

    public static final String PREFS_NAME = "prefs";
    public static final String KEY_TOEKN = "token";

    public static boolean isCurrentLogined() {
        return KiiUser.getCurrentUser() != null;
    }

    public static void saveToken(Context context, String token) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME,
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(KEY_TOEKN, token);
        e.commit();
    }

    public static String convertTimestampToString(long mill){
        Date date=new Date(mill);
        String strs="";
        try {
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            strs=sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strs;
    }
}
