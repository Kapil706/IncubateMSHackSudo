package com.sudo.campusambassdor.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtil {

    private static final String TAG = PreferencesUtil.class.getSimpleName();

    private static final String SHARED_PREFERENCES_NAME = "com.mshack.sudo.campusambassdors3";
    private static final String PREF_PARAM_IS_PROFILE_CREATED = "isProfileCreated";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static Boolean isProfileCreated(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_PROFILE_CREATED, false);
    }

    public static void setProfileCreated(Context context, Boolean isProfileCreated) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_PROFILE_CREATED, isProfileCreated).commit();
    }

    public static void clearPreferences(Context context){
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.clear();
        editor.apply();
    }
}
