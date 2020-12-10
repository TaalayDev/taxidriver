package kg.dos2.taxidriver;

import android.content.Context;
import android.content.SharedPreferences;

class Prefs {

    final static String SHARED_PREF = "SHARED_PREF";

    final static boolean DEFAULT_BOOLEAN = false;
    final static String DEFAULT_STRING = "";
    final static int DEFAULT_INT = 0;
    final static long DEFAULT_LONG = 0;

    final static String DRIVER_ID = "DRIVER_ID";
    final static String DRIVER = "DRIVER";
    final static String FIRST_NAME = "FIRST_NAME";
    final static String LAST_NAME = "LAST_NAME";
    final static String PHONE = "PHONE";
    final static String MY_TRF = "MY_TRF";
    final static String PSW = "PSW";
    final static String BALANCE = "BALANCE";
    final static String IMAGE = "IMAGE";
    final static String CITY = "CITY";

    final static String DPAYD = "DPAY";

    static final String PERMISSIONS_GRANTED = "PERMISSIONS_GRANTED";
    static final String APP_STATE = "APP_STATE";
    static final String ORDER_ID = "ORDER_ID";
    static final String RATSIA = "RATSIA";

    static final String IS_SOS = "IS_SOS";

    static final String IS_LOGIN = "IS_LOGIN";

    static final String BASE_URL = "http://taalaydev.000webhostapp.com/testtaxi/cpanel/api/";
    static final String URL_CHECK_LOGIN = BASE_URL + "check_login/";
    static final String URL_ORDER_INFO = BASE_URL + "order_info/";
    static final String URL_DRIVER_INFO = BASE_URL + "driver_info/";
    static final String URL_CANC_ORDER = BASE_URL + "canc_ord/";
    static final String URL_COMPLETE_ORDER = BASE_URL + "compl_ord/";
    static final String URL_TRY_GET_ORDER = BASE_URL + "try_get_order/";
    static final String URL_GET_ACTIVE_ORDERS = BASE_URL + "get_active_orders/";
    static final String URL_GET_ACTIVE_ORDERS_BY_CATEGORY = BASE_URL + "get_active_orders_by_cat/";
    static final String URL_GET_CATEGORIES = BASE_URL + "get_cat/";
    static final String URL_UPDATE_DRIVER_STATE = BASE_URL + "update_driver_state/";
    static final String URL_UPDATE_DRIVER_LAT_LNG = BASE_URL + "update_driver_lat_lng/";
    static final String URL_CHECK_IS_SOS_SEND = BASE_URL + "check_is_sos_send/";
    static final String URL_GET_DISP_LIST = BASE_URL + "get_disp_list/";
    static final String URL_CHECK_CHAT = BASE_URL + "get_mess/";
    static final String URL_SEND_MESS = BASE_URL + "send_mess/";
    static final String URL_START_WLK = BASE_URL + "start_wlk/";
    static final String URL_CHECK_DPAY = BASE_URL + "check_d_pay/";
    static final String URL_TRF_INFO = BASE_URL + "trf_info/";

    static boolean getBoolean(Context c, String key) {
        SharedPreferences sp = c.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        return sp.getBoolean(key, DEFAULT_BOOLEAN);
    }
    
    static boolean getBooleanDef(Context c, String key, boolean def_bool) {
        SharedPreferences sp = c.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        return sp.getBoolean(key, def_bool);
    }

    static String getString(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        return sp.getString(key, DEFAULT_STRING);
    }
    static String getStringDef(Context context, String key, String def) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        return sp.getString(key, def);
    }
    static int getInt(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        return sp.getInt(key, DEFAULT_INT);
    }
    static int getIntDef(Context context, String key, int def) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        return sp.getInt(key, def);
    }
    static long getLong(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        return sp.getLong(key, DEFAULT_LONG);
    }

    static void putBoolean(Context c, String key, boolean value) {
        SharedPreferences sp = c.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, value).commit();
    }
    static void putString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        sp.edit().putString(key, value).commit();
    }
    static void putInt(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        sp.edit().putInt(key, value).commit();
    }

    static void putLong(Context context, String key, long value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        sp.edit().putLong(key, value).commit();
    }

    static void putFloat(Context context, String key, float value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        sp.edit().putFloat(key, value).commit();
    }

    static float getFloat(Context context, String key, float value) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREF, context.MODE_PRIVATE);
        return sp.getFloat(key, value);
    }

}
