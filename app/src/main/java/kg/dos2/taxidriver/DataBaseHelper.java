package kg.dos2.taxidriver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class DataBaseHelper extends SQLiteOpenHelper {

    DataBaseHelper(Context context) {
        super(context, "kg_dos2_taxi_driver.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `order` (`id` INTEGER, `addr1` TEXT, `addr2` TEXT, " +
                "`desc` TEXT, `phone` TEXT, `cat` INTEGER, `cost` INTEGER, `dist` REAL, " +
                "`lat1` REAL, `lng1` REAL, `lat2` REAL, `lng2` REAL);");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (id INTEGER, name TEXT, parent INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat` (`id` INTEGER, `mCode` INTEGER, `sender` INTEGER, " +
                "`getter` INTEGER, `senderLogin` TEXT, `getterLogin` TEXT, `message` TEXT, `date` TEXT, `getterIsRead` INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reports` (`id` INTEGER, `addr1` TEXT, `addr2` TEXT, " +
                "`desc` TEXT, `phone` TEXT, `cat` TEXT, `cost` INTEGER, `dist` REAL, " +
                "`date` TEXT, `state` INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS `order`;");
        db.execSQL("DROP TABLE IF EXISTS `categories`;");
        db.execSQL("DROP TABLE IF EXISTS `chat`;");
        db.execSQL("DROP TABLE IF EXISTS `reports`;");
    }

    public void newReport(Order order, int state) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String currentDateTime = dateFormat.format(new Date());
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", order.getId());
        values.put("addr1", order.getAddr1());
        values.put("addr2", order.getAddr2());
        values.put("desc", order.getDesc());
        values.put("phone", order.getPhone());
        values.put("cat", order.getCat());
        values.put("cost", order.getCost());
        values.put("dist", order.getDist());
        values.put("date", currentDateTime);
        values.put("state", state);
        db.insert("reports", null, values);
        db.close();
    }

    public void insertOrder(Order order) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", order.getId());
        values.put("addr1", order.getAddr1());
        values.put("addr2", order.getAddr2());
        values.put("desc", order.getDesc());
        values.put("phone", order.getPhone());
        values.put("cat", order.getCat());
        values.put("cost", order.getCost());
        values.put("dist", order.getDist());
        values.put("lat1", order.getLat1());
        values.put("lng1", order.getLng1());
        values.put("lat2", order.getLat2());
        values.put("lng2", order.getLng2());
        db.insert("order", null, values);
        db.close();
    }

    public void insertMessage(Bundle bundle) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", bundle.getInt("id"));
        values.put("mCode", bundle.getInt("mCode"));
        values.put("sender", bundle.getInt("sender"));
        values.put("getter", bundle.getInt("getter"));
        values.put("senderLogin", bundle.getString("senderLogin"));
        values.put("getterLogin", bundle.getString("getterLogin"));
        values.put("message", bundle.getString("message"));
        values.put("date", bundle.getString("date"));
        values.put("getterIsRead", bundle.getInt("getterIsRead"));
        db.insert("chat", null, values);
        db.close();
    }

    public Order getCurrentOrder() {
        Order order = new Order();
        try {
            SQLiteDatabase db = getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM `order`", null);
            if (cursor.moveToNext()) {
                order.setId(cursor.getInt(cursor.getColumnIndex("id")));
                order.setAddr1(cursor.getString(cursor.getColumnIndex("addr1")));
                order.setAddr2(cursor.getString(cursor.getColumnIndex("addr2")));
                order.setDesc(cursor.getString(cursor.getColumnIndex("desc")));
                order.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
                order.setCat(cursor.getInt(cursor.getColumnIndex("cat")));
                order.setCost(cursor.getInt(cursor.getColumnIndex("cost")));
                order.setDist(cursor.getFloat(cursor.getColumnIndex("dist")));
                order.setLat1(cursor.getFloat(cursor.getColumnIndex("lat1")));
                order.setLng1(cursor.getFloat(cursor.getColumnIndex("lng1")));
                order.setLat2(cursor.getFloat(cursor.getColumnIndex("lat2")));
                order.setLng2(cursor.getFloat(cursor.getColumnIndex("lng2")));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "getCurrentOrderException " + e.toString());
        }
        return order;
    }

    public void deleteOrder(Order order) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM `order` WHERE `id` = " + order.getId());
        db.close();
    }

}
