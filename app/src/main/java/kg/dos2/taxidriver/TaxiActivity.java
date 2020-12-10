package kg.dos2.taxidriver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static kg.dos2.taxidriver.Prefs.URL_CANC_ORDER;
import static kg.dos2.taxidriver.Prefs.URL_CHECK_CHAT;
import static kg.dos2.taxidriver.Prefs.URL_CHECK_DPAY;
import static kg.dos2.taxidriver.Prefs.URL_CHECK_IS_SOS_SEND;
import static kg.dos2.taxidriver.Prefs.URL_COMPLETE_ORDER;
import static kg.dos2.taxidriver.Prefs.URL_DRIVER_INFO;
import static kg.dos2.taxidriver.Prefs.URL_ORDER_INFO;
import static kg.dos2.taxidriver.Prefs.URL_START_WLK;
import static kg.dos2.taxidriver.Prefs.URL_TRF_INFO;
import static kg.dos2.taxidriver.Prefs.URL_TRY_GET_ORDER;
import static kg.dos2.taxidriver.Prefs.URL_UPDATE_DRIVER_LAT_LNG;
import static kg.dos2.taxidriver.Prefs.URL_UPDATE_DRIVER_STATE;

public class TaxiActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        NavigationView.OnNavigationItemSelectedListener, FragmentsListener,
        LocationUtils.MyLocationListener {
    Intent taxiIntent;
    private PermissionsManager permissionsManager;
    private Location mLastLocation;

    public static final String LOG_TAG = "TAXI_DRIVER_LOG_TAG";
    private final int PERMISSIONS_REQUESTS_CODE = 3;
    // Идентификатор уведомления
    private static final int NEW_MESSAGE_NOTIFY_ID = 101;
    // Идентификатор канала
    private static String NEW_MESSAGE_CHANNEL_ID = "Cat channel";

    Button buttonRatsia, buttonOrderStreet, buttonSOS;
    NavigationView navigationView;
    TextView textbalance;

    public static final int STATE_OFF = 0;
    public static final int STATE_ON = 1;
    public static final int STATE_SYNCED_TO_ORDER = 2;
    public static final int STATE_CARRIAGE = 3;
    public static final int STATE_TAKE_ORDER_FROM_STREET = 4;
    public static final int STATE_CARRIAGE_CLIENT_FROM_STREET = 5;

    public static final int FRAGMENT_ORDER_LIST = 0;
    public static final int FRAGMENT_CHAT = 1;
    public static final int FRAGMENT_MAP = 2;
    public static final int FRAGMENT_CLIENT_ORDER = 3;
    public static final int FRAGMENT_TAXOMETER = 4;
    public static final int FRAGMENT_NEWS = 5;
    public static final int FRAGMENT_SOS = 6;

    int orderid = 0, currentFragment = 0;
    public static volatile boolean ratsiaOff = false;
    boolean isLongClicked = false, audio_message = false;
    double lat = 0, lng = 0;

    MediaRecorder recorder;
    OrdersListFragment ordersListFragment;
    TaxometerFragment taxometerFragment;
    MapFragment mapFragment;
    NewsFragment newsFragment;
    ClientOrderFragment clientOrderFragment;
    ChatFragment chatFragment;
    DataBaseHelper dbhelper;
    // private GPSTracker gps;
    private StorageReference storageReference;
    Firebase wfb = null;

    class Trf {
        int id = 0;
        String name = "";
        int tk_ord = 0;
        int trf_km = 0;
        int lnd = 0;
        int tk_ord_fs = 0;
        int wt = 0;
        Trf(int id, String name, int tk_ord, int trf_km, int lnd, int tk_ord_fs, int wt) {
            this.id = id;
            this.name = name;
            this.tk_ord = tk_ord;
            this.trf_km = trf_km;
            this.lnd = lnd;
            this.tk_ord_fs = tk_ord_fs;
            this.wt = wt;
        }
    }
    public static ArrayList<Trf> tariffList = new ArrayList<>();

    private LocationUtils locationUtils;

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        locationUtils.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        locationUtils.onStart();
    }

    public void checkPermission() {
        // Here, thisActivity is the current activity
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUESTS_CODE);

        } else {
            // Permission has already been granted
        }
    }

    public void startTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Date d = new Date();
                        if (((d.getSeconds() % 5) == 0)) {
                            request(0, 0);
                            // if ( gps == null )
                                // gps = new GPSTracker(TaxiActivity.this);

                            if ( locationUtils.canGetLocation() ) {
                                lat = getLatitude();
                                lng = getLongitude();
                                Log.e(LOG_TAG, "gps lat: " + lat + "; lng: " + lng);
                                request(6, 0);
                            } else {
                                togglePeriodicLocationUpdates();
                            }
                        }
                        if (((d.getSeconds() % 3) == 0)) {
                            request(8, 0);
                            if ( getState() == STATE_SYNCED_TO_ORDER )
                                request(2, 1);
                        }
                    }
                });
            }
        }, 10, 1000L);
    }

    public void request(final int code, final int temp) {
        String url = URL_DRIVER_INFO;
        if ( code == 2 ) url = URL_ORDER_INFO;
        else if ( code == 3 ) url = URL_CANC_ORDER;
        else if ( code == 4 ) url = URL_COMPLETE_ORDER;
        else if ( code == 5 ) url = URL_UPDATE_DRIVER_STATE;
        else if ( code == 6 ) url = URL_UPDATE_DRIVER_LAT_LNG;
        else if ( code == 7 ) url = URL_CHECK_IS_SOS_SEND;
        else if ( code == 8 ) url = URL_CHECK_CHAT;
        else if ( code == 9 ) url = URL_START_WLK;
        else if ( code == 10 ) url = URL_CHECK_DPAY;
        else if ( code == 11 ) url = URL_TRF_INFO;

        final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    // Log.e(LOG_TAG, "taxiactivity request -> code: " + code + "; response:  " + s);
                    if ( code == 0 ) {
                        JSONArray arr = new JSONArray(s);
                        // Log.e("myimag", "products size - " + arr.length());
                        for ( int i = 0; i < arr.length(); i++ ) {
                            JSONObject obj = arr.getJSONObject(i);
                            int state = obj.getInt("State");
                            if ( state != -1 ) {
                                Prefs.putInt(TaxiActivity.this, Prefs.BALANCE, obj.getInt("Balance"));
                                String balance = "" + obj.getInt("Balance") + "СОМ";
                                textbalance.setText(balance);
                                if ( currentFragment == FRAGMENT_ORDER_LIST ) {
                                    ordersListFragment.balanceChanged();
                                }
                                request(11, 0);
                                if ( Prefs.getIntDef(TaxiActivity.this, Prefs.DPAYD, 0) == 0 &&
                                        obj.getInt("Balance") > 0 ) {
                                    request(10, 0);
                                }
                            } else {

                            }
                        }
                    } else if ( code == 2 ) {
                        try {
                            JSONArray arr = new JSONArray(s);
                            // Log.e("myimag", "products size - " + arr.length());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                final int idd = obj.getInt("ID");
                                final int tp = obj.getInt("tp");
                                final int state = obj.getInt("State");
                                String addr1 = obj.getString("Addr1");
                                String addr2 = obj.getString("Addr2");
                                String desc = obj.getString("Desc");
                                String phone = obj.getString("Phone");
                                String date = obj.getString("date_time");
                                double lat1 = obj.getDouble("lat1");
                                double lng1 = obj.getDouble("lng1");
                                double lat2 = obj.getDouble("lat2");
                                double lng2 = obj.getDouble("lng2");
                                double dist = obj.getDouble("dist");
                                int cost = obj.getInt("cost");
                                int cat = obj.getInt("cat");
                                int c_id = obj.getInt("client");

                                Order order = new Order(1, 1, 100, 12f, 72.0000F,
                                        48.0000F, 0, (float) lng2, addr1, addr2, phone, desc, date, c_id);
                                order.setTp(tp);
                                Order.current = order;
                                dbhelper.insertOrder(order);
                            }
                        } catch (JSONException je) {
                            Log.e(LOG_TAG, je.toString());
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.toString());
                        }
                        if ( temp == 0 )
                            replaceFragment(taxometerFragment);
                    } else if ( code == 3 || code == 4 ) {
                        if ( !s.equals("-1") ) {
                            dbhelper.deleteOrder(Order.current);
                            Order.current = null;
                            Prefs.putInt(TaxiActivity.this, Prefs.APP_STATE, STATE_ON);
                            Prefs.putInt(TaxiActivity.this, Prefs.ORDER_ID, 0);
                            if ( code == 3 ) replaceFragment(ordersListFragment);
                            try {
                                Firebase.setAndroidContext(TaxiActivity.this);
                                new Firebase("https://dos-f9894.firebaseio.com/order")
                                        .child("state").setValue(System.currentTimeMillis());
                            } catch ( Exception e ) {
                                e.printStackTrace();
                            }
                            buttonOrderStreet.setText("С улицы");
                            buttonOrderStreet.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_pan_tool, 0, 0);
                        }
                    } else if ( code == 7 ) {
                        try {
                            JSONArray arr = new JSONArray(s);
                            Log.e(LOG_TAG, "warn response - " + s);
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if ( obj.getInt("id") != Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0) ) {
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("id", obj.getInt("id"));
                                    bundle.putInt("dvid", obj.getInt("dvid"));
                                    bundle.putDouble("lat", obj.getDouble("lat"));
                                    bundle.putDouble("lng", obj.getDouble("lng"));
                                    bundle.putInt("state", obj.getInt("state"));
                                    bundle.putString("phone", obj.getString("phone"));
                                    String phone = obj.getString("phone");
                                    bundle.putInt("c", obj.getInt("c"));
                                    LinearLayout layout = new LinearLayout(TaxiActivity.this);
                                    layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT));
                                    layout.setOrientation(LinearLayout.VERTICAL);
                                    View view = getLayoutInflater().inflate(R.layout.warn_alert_dialog_item, layout, false);
                                    TextView warn_alert_text = view.findViewById(R.id.warn_alert_text);
                                    warn_alert_text.setText("Внимание\n позывной #" + obj.getInt("dvid") + " послал сигнал тревоги!");
                                    Button warn_alert_btn_call = view.findViewById(R.id.warn_alert_btn_call);
                                    Button warn_alert_btn_map = view.findViewById(R.id.warn_alet_btn_to_map);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(TaxiActivity.this);
                                    layout.addView(view);
                                    builder.setView(layout);
                                    AlertDialog dialog = builder.show();
                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    // Vibrate for 500 milliseconds
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                                    } else {
                                        //deprecated in API 26
                                        v.vibrate(500);
                                    }
                                    try {
                                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                        r.play();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    warn_alert_btn_call.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (dialog != null)
                                                dialog.cancel();
                                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                                            startActivity(intent);
                                            SOSFragment sosFragment = SOSFragment.newInstance(bundle);
                                            sosFragment.setCallBack(TaxiActivity.this);
                                            replaceFragment(sosFragment);
                                        }
                                    });
                                    warn_alert_btn_map.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (dialog != null)
                                                dialog.cancel();
                                            SOSFragment sosFragment = SOSFragment.newInstance(bundle);
                                            sosFragment.setCallBack(TaxiActivity.this);
                                            replaceFragment(sosFragment);
                                        }
                                    });
                                }
                            }

                        } catch (Exception e) {

                        }
                    } else if ( code == 8 ) {
                        try {
                            JSONArray arr = new JSONArray(s);
                            // Log.e(LOG_TAG, "chat response - " + s);
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if ( obj.getInt("m_code") == 1 || obj.getInt("m_code") == 4 ) {
                                    String sender = "Диспетчер";
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("id", obj.getInt("id"));
                                    bundle.putInt("mCode", obj.getInt("m_code"));
                                    if ( obj.getInt("m_code") == 1 ) {
                                        bundle.putInt("sender", obj.getInt("disp"));
                                        bundle.putInt("getter", obj.getInt("drv"));
                                        bundle.putString("senderLogin", obj.getString("disp_login"));
                                        bundle.putString("getterLogin", obj.getString("drv_login"));
                                    } else {
                                        sender = "Клиент";
                                        bundle.putInt("sender", obj.getInt("client"));
                                        bundle.putInt("getter", obj.getInt("drv"));
                                        bundle.putString("senderLogin", obj.getString("client_login"));
                                        bundle.putString("getterLogin", obj.getString("drv_login"));
                                    }
                                    bundle.putString("message", obj.getString("mess"));
                                    bundle.putString("date", obj.getString("date"));
                                    bundle.putInt("getterIsRead", obj.getInt("drv_is_read"));
                                    dbhelper.insertMessage(bundle);
                                    if (currentFragment == FRAGMENT_CHAT) {
                                        chatFragment.showMessage(bundle);
                                    } else {
                                        if ( sender.equals("Диспетчер") )
                                            setMenuCounter(R.id.nav_dispatcher, 1);
                                        not(sender, obj.getString("mess"), NEW_MESSAGE_CHANNEL_ID, NEW_MESSAGE_NOTIFY_ID);
                                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                        // Vibrate for 500 milliseconds
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                                        } else {
                                            //deprecated in API 26
                                            v.vibrate(500);
                                        }
                                    }
                                    try {
                                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                        r.play();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (JSONException jse) {

                        } catch (Exception e) {

                        }
                    } else if ( code == 10 ) {
                        JSONObject jobj = new JSONObject(s);
                        int bal = jobj.getInt("bal");
                        Prefs.putInt(TaxiActivity.this, Prefs.BALANCE, bal);
                        int dpay = jobj.getInt("payd");
                        Prefs.putInt(TaxiActivity.this, Prefs.DPAYD, dpay);
                        String balance = "" + bal + "СОМ";
                        textbalance.setText(balance);
                        if ( currentFragment == FRAGMENT_ORDER_LIST ) {
                            ordersListFragment.balanceChanged();
                        }
                    } else if ( code == 11 ) {
                        JSONArray arr = new JSONArray(s);
                        tariffList.clear();
                        // Log.e("myimag", "products size - " + arr.length());
                        for ( int i = 0; i < arr.length(); i++ ) {
                            JSONObject obj = arr.getJSONObject(i);
                            tariffList.add(new Trf(
                                    obj.getInt("ID"),
                                    obj.getString("Tariff"),
                                    obj.getInt("TariffTo"),
                                    obj.getInt("TariffKm"),
                                    obj.getInt("TariffLand"),
                                    obj.getInt("TariffToS"),
                                    obj.getInt("TariffWaiting"))
                            );
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("myimag", "JSONException " + e);
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
                request(code, temp);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                if ( code == 0 ) {
                    params.put("id", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0)));
                    params.put("state", String.valueOf(getState()));
                } else if ( code == 2 || code == 3 || code == 4 || code == 9 ) {
                    params.put("id", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.ORDER_ID, 0)));
                    params.put("driver", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0)));
                    params.put("state", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.APP_STATE, STATE_OFF)));
                    if ( code == 4 ) {
                        params.put("ord_m", String.valueOf(temp));
                        if ( Order.current != null )
                            params.put("ord_w", String.valueOf(Order.current.getWmin()));
                    }
                } else if ( code == 5 ) {
                    params.put("id", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0)));
                    params.put("state", String.valueOf(temp));
                } else if ( code == 6 ) {
                    params.put("id", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0)));
                    params.put("lat", String.valueOf(lat));
                    params.put("lng", String.valueOf(lng));
                } else if ( code == 7 ) {
                    params.put("id", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0)));
                } else if ( code == 8 || code == 10 ) {
                    params.put("id", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0)));
                    params.put("act", "1");
                } else if ( code == 11 ) {
                    params.put("city", String.valueOf(Prefs.getIntDef(TaxiActivity.this, Prefs.CITY, 1)));
                }
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    void not(String title, String text, String chanel_id, int notify_id) {
        NotificationManager mNotificationManager;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), chanel_id);
        Intent ii = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(TaxiActivity.this, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(text);
        bigText.setBigContentTitle(title);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setStyle(bigText);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = chanel_id;
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    title,
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mNotificationManager.notify(notify_id, mBuilder.build());
    }

    private void setTariffList() {
        for ( int i = 0; i < 4; i++ )
            tariffList.add(new Trf(i, "Tariff " + i, i * 2 + 20, i * 2 + 10,
                    i * 2 + 30, i * 6, i * 2 + 4)
            );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxi);

        checkPermission();
        // not("TITLE", "MESSAGE", NEW_MESSAGE_CHANNEL_ID, NEW_MESSAGE_NOTIFY_ID);
        dbhelper = new DataBaseHelper(TaxiActivity.this);
        taxiIntent = new Intent(TaxiActivity.this, ServerConnectService.class);
        taxiIntent.putExtra("pIntent", createPendingResult(0, new Intent(), 0));
        startService(taxiIntent);

        File checkDir = new File(Environment.getExternalStorageDirectory() + "/AutoTaxi/Messages/Audio");
        if (!checkDir.exists()) {
            checkDir.mkdirs();
            //new File(Environment.getExternalStorageDirectory() + "/AutoTaxi/Messages");
        }

        storageReference = FirebaseStorage.getInstance().getReference();

        buttonRatsia = findViewById(R.id.button_ratsia);
        buttonRatsia.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if ( !ratsiaOff ) {
                    // Log.e("mytaxi", "Longpress detected");
                    // send("recordingToTalkieWalkie");
                    isLongClicked = true;
                    buttonRatsia.setText("Запись");
                    buttonRatsia.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_action_zapis), null, null);
                    record();
                }
                return false;
            }
        });

        buttonRatsia.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        break;
                    case MotionEvent.ACTION_MOVE:

                        break;
                    case MotionEvent.ACTION_UP:
                        if (isLongClicked) {
                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isLongClicked = false;
                                    stopRecord();
                                    // ServerConnectService.talkieWalkie();
                                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/lastSendTalkieWalkieRecordFile.mp3");
                                    uploadFile(Uri.fromFile(file));
                                    buttonRatsia.setText("Рация");
                                    buttonRatsia.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_mic), null, null);
                                }
                            }, 500);
                        } else {
                            if (!ratsiaOff) {
                                ratsiaOff = true;
                                buttonRatsia.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_mic_off), null, null);
                                Prefs.putBoolean(TaxiActivity.this, Prefs.RATSIA, ratsiaOff);
                            } else {
                                ratsiaOff = false;
                                buttonRatsia.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_mic), null, null);
                                Prefs.putBoolean(TaxiActivity.this, Prefs.RATSIA, ratsiaOff);
                            }
                        }
                        break;
                }
                return false;
            }

        });

        initFbListener();

        buttonSOS = findViewById(R.id.button_sos);
        buttonSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(TaxiActivity.this, "Долгое нажатие активирует тревогу!", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSOS.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TaxiActivity.this);
                builder.setTitle("Послать сигнал тревоги?");
                builder.setMessage("Внимание вы действительно хотите послать сигнал тревоги?");
                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Firebase.setAndroidContext(TaxiActivity.this);
                        new Firebase("https://dos-f9894.firebaseio.com/warn")
                                .child("drv").setValue(Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0));
                        Prefs.putBoolean(TaxiActivity.this, Prefs.IS_SOS, true);
                        SOSFragment sosFragment = new SOSFragment();
                        sosFragment.setCallBack(TaxiActivity.this);
                        replaceFragment(sosFragment);
                    }
                });
                builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                return true;
            }
        });

        buttonOrderStreet = findViewById(R.id.button_order_street);
        buttonOrderStreet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(LOG_TAG, "btordstrclck");
                if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_CARRIAGE ) {
                    taxometerFragment.openOrCloseBottomSheet();
                }
                if ( getState() == STATE_OFF || getState() == STATE_ON ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TaxiActivity.this);
                    builder.setTitle("Взять заказ с улицы?");
                    builder.setMessage("Вы действительно хотите взять заказ с улицы?");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            tryConnectToOrder(0);
                        }
                    });
                    builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                }
            }
        });

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        textbalance = navigationView.getHeaderView(0).findViewById(R.id.textView);
        TextView driver_name = navigationView.getHeaderView(0).findViewById(R.id.driver_name);
        CircularImageView driver_image = navigationView.getHeaderView(0).findViewById(R.id.driver_image);
        String balance = "" + Prefs.getInt(this, Prefs.BALANCE) + "СОМ";
        textbalance.setText(balance);
        driver_name.setText(Prefs.getStringDef(this, Prefs.DRIVER, "Водитель"));
        if ( !Prefs.getStringDef(this, Prefs.IMAGE, "").isEmpty() ) {
            new DownloadImageTask(driver_image).execute(Prefs.BASE_URL.replace("/api/", "")
                    + "/img/drivers/" + Prefs.getStringDef(this, Prefs.IMAGE, ""));
        }

        locationUtils = new LocationUtils(this, this);
        togglePeriodicLocationUpdates();

        ratsiaOff = Prefs.getBoolean(this, Prefs.RATSIA);
        startTimer();

        ordersListFragment = new OrdersListFragment();
        ordersListFragment.setCallBack(this);
        clientOrderFragment = new ClientOrderFragment();
        clientOrderFragment.setCallBack(this);
        mapFragment = new MapFragment();
        mapFragment.setCallBack(this);
        newsFragment = new NewsFragment();
        newsFragment.setCallBack(this);
        taxometerFragment = new TaxometerFragment();
        taxometerFragment.setCallBack(this);
        chatFragment = ChatFragment.newInstance(this, new Bundle());
        checkState();
        request(11, 0);
    }

    public void checkState() {
        if ( getState() == STATE_OFF || getState() == STATE_ON ) {
            replaceFragment(ordersListFragment);
            Prefs.putInt(this, Prefs.APP_STATE, STATE_ON);
        } else if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_TAKE_ORDER_FROM_STREET ) {
            /*buttonOrderStreet.setText("О заказе");
            buttonOrderStreet.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_info, 0, 0);
            Order.current = dbhelper.getCurrentOrder();
            // replaceFragment(taxometerFragment);*/
            request(2, 0);
        } else if ( getState() == STATE_CARRIAGE || getState() == STATE_CARRIAGE_CLIENT_FROM_STREET ) {
            /*buttonOrderStreet.setText("О заказе");
            buttonOrderStreet.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_info, 0, 0);
            Order.current = dbhelper.getCurrentOrder();*/
            // replaceFragment(taxometerFragment);
            request(2, 0);
        }
        request(5, getState());
    }

    ChildEventListener ch = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            try {
                int val = Integer.parseInt(dataSnapshot.getValue().toString());
                if ( val == 0 ) {

                } else if ( val == -1 ) {
                    request(7, 0);
                }
            } catch (Exception e) {

            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    };

    public void initFbListener() {
        Firebase.setAndroidContext(TaxiActivity.this);
        wfb = new Firebase("https://dos-f9894.firebaseio.com/warn");
        wfb.addChildEventListener(ch);
    }

    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {

    }

    void currentOrderInfoDialogShow() {
        if ( Order.current != null ) {
            final AlertDialog alertDialog;
            AlertDialog.Builder abuilder = new AlertDialog.Builder(TaxiActivity.this);

            LinearLayout lay = new LinearLayout(this);
            lay.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            View view = getLayoutInflater().inflate(R.layout.order_info_dialog_layout, lay, false);

            TextView tvaddr1 = view.findViewById(R.id.tvaddr1);
            tvaddr1.setText(Order.current.getAddr1());

            TextView tvaddr2 = view.findViewById(R.id.tvaddr2);
            tvaddr2.setText(Order.current.getAddr2());

            if (Order.current.getDesc().isEmpty()) {
                view.findViewById(R.id.linearLayout3).setVisibility(View.GONE);
            } else {
                TextView tvdesc = view.findViewById(R.id.tvdesc);
                tvdesc.setText(Order.current.getDesc());
            }

            TextView tvphone = view.findViewById(R.id.tvphone);
            tvphone.setText(Order.current.getPhone());

            ImageView buttonClose = view.findViewById(R.id.UClose);

            lay.addView(view);
            abuilder.setView(lay);
            alertDialog = abuilder.show();

            buttonClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (alertDialog != null)
                        alertDialog.cancel();
                }
            });
        }
    }

    public void onViewClick(View v) {
        PopupMenu pmenu = new PopupMenu(this, v);
        MenuInflater inflater = pmenu.getMenuInflater();
        switch(v.getId()) {
            case R.id.button_menu:
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.openDrawer(GravityCompat.START);
                break;
            case R.id.button_sos:
                Toast.makeText(this, "SOS", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_order_street:
                String bosText = buttonOrderStreet.getText().toString();
                Log.e(LOG_TAG, "btordstrclck " + getState() + " ");

                break;
        }
    }

    @SuppressLint("MissingSuperCall")
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LayoutInflater inflater = getLayoutInflater();
        View view = null;
        if ( resultCode == 1 ) {

        } else if ( resultCode == 2 ) {
            try {
                //ordersListFragment.clearList();
                String rs = data.getStringExtra("active_order_list");
                JSONArray arr = new JSONArray(rs);
                Log.e("myimag", "products size - " + arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Order order = new Order();
                    order.setId(obj.getInt("ID"));
                    order.setAddr1(obj.getString("Addr1"));
                    order.setAddr2(obj.getString("Addr2"));
                    order.setDesc(obj.getString("Desc"));
                    order.setCat(obj.getInt("cat"));
                    order.setTp(obj.getInt("tp"));
                    order.setCost(obj.getInt("cost"));
                    order.setDist((float) obj.getDouble("dist"));
                    ordersListFragment.addOrder(order);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        } else if ( resultCode == 3 ) {
            if ( currentFragment == FRAGMENT_ORDER_LIST ) {
                ordersListFragment.ordersChanged();
            }
        } else if ( resultCode == 4 ) {
            String rs = data.getStringExtra("order");
            try {
                JSONObject jobj = new JSONObject(rs);
                Log.e(LOG_TAG, "resultCode 4 " + rs);
                final int idd = jobj.getInt("ID");
                Prefs.putInt(TaxiActivity.this, Prefs.ORDER_ID, idd);

                LinearLayout layout = new LinearLayout(TaxiActivity.this);
                layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.VERTICAL);

                View v = getLayoutInflater().inflate(R.layout.w_ord, layout, false);

                TextView nordtxt = v.findViewById(R.id.nwordaddr1);
                nordtxt.setText(jobj.getString("Addr1"));

                if ( jobj.getString("Addr2").isEmpty() || jobj.getString("Addr2").equals("") ) {
                    v.findViewById(R.id.nwordlay2).setVisibility(View.GONE);
                    v.findViewById(R.id.divider6).setVisibility(View.GONE);
                } else {
                    v.findViewById(R.id.nwordlay2).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.divider6).setVisibility(View.VISIBLE);
                    TextView nordaddr2 = v.findViewById(R.id.nwordaddr2);
                    nordaddr2.setText(jobj.getString("Addr2"));
                }
                if ( jobj.getString("Desc").isEmpty() || jobj.getString("Desc").equals("") ) {

                } else {

                }
                if ( jobj.getInt("cost") == 0 ) {
                    v.findViewById(R.id.nwordprice).setVisibility(View.GONE);
                } else {
                    TextView nordp = v.findViewById(R.id.nwordprice);
                    nordp.setText(String.format(Locale.getDefault(), "%d C", jobj.getInt("cost")));
                }
                TextView nwordcat = v.findViewById(R.id.nwordcat);
                nwordcat.setText("");



                Button btn_ok = v.findViewById(R.id.btn_ok);
                Button btn_canc = v.findViewById(R.id.btn_canc);
                AlertDialog.Builder builder = new AlertDialog.Builder(TaxiActivity.this);
                layout.addView(v);
                builder.setView(layout);
                AlertDialog dialog = builder.show();
                Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    vib.vibrate(500);
                }
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                btn_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (dialog != null)
                            dialog.cancel();
                        tryConnectToOrder(idd);
                    }
                });
                btn_canc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (dialog != null)
                            dialog.cancel();
                        cancOrd();
                    }
                });
            } catch (JSONException jse) {
                Log.e(LOG_TAG, "resultCode4JExc " + jse.toString());
            } catch (Exception e) {
                Log.e(LOG_TAG, "resultCode4Exc " + e.toString());
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    protected void onStop() {
        super.onStop();
        /*if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }*/
        request(5, 0);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        request(5, getState());
    }

    @Override
    public void onDestroy()
    {
        stopService(new Intent(TaxiActivity.this, ServerConnectService.class));
        if ( getState() == STATE_ON ) {
            Prefs.putInt(this, Prefs.APP_STATE, STATE_OFF);
        }
        lat = lng =  0;
        request(5, 0);
        request(6, 0);
        super.onDestroy();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_orders) {
            if ( getState() != STATE_SYNCED_TO_ORDER && getState() != STATE_CARRIAGE
                    && getState() != STATE_TAKE_ORDER_FROM_STREET && getState() != STATE_CARRIAGE_CLIENT_FROM_STREET )
                replaceFragment(ordersListFragment);
        } else if (id == R.id.nav_dispatcher) {
            if ( getState() != STATE_SYNCED_TO_ORDER && getState() != STATE_CARRIAGE
                    && getState() != STATE_TAKE_ORDER_FROM_STREET && getState() != STATE_CARRIAGE_CLIENT_FROM_STREET ) {
                chatFragment = ChatFragment.newInstance(this, new Bundle());
                chatFragment.show(getSupportFragmentManager(), "CHAT_FRAGMENT_TAG");
                setMenuCounter(R.id.nav_dispatcher, 0);
                // replaceFragment(chatFragment);
            }
        } else if (id == R.id.nav_map) {
            if ( getState() != STATE_SYNCED_TO_ORDER && getState() != STATE_CARRIAGE
                    && getState() != STATE_TAKE_ORDER_FROM_STREET && getState() != STATE_CARRIAGE_CLIENT_FROM_STREET )
                replaceFragment(mapFragment);
        } else if (id == R.id.nav_otchety) {
            if ( getState() != STATE_SYNCED_TO_ORDER && getState() != STATE_CARRIAGE
                    && getState() != STATE_TAKE_ORDER_FROM_STREET && getState() != STATE_CARRIAGE_CLIENT_FROM_STREET )
                replaceFragment(newsFragment);
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=kg.dos2.taxidriver");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent,"Поделиться"));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return false;
    }

    private void setMenuCounter(@IdRes int itemId, int count) {
        try {
            Menu menuNav = navigationView.getMenu();
            MenuItem element = menuNav.findItem(itemId);
            if ( itemId == R.id.nav_dispatcher ) {
                String before = "Диспетчерская";
                if ( count > 0 ) {
                    String counter = Integer.toString(count);
                    String s = before + "   " + counter + " ";
                    SpannableString sColored = new SpannableString(s);

                    sColored.setSpan(new BackgroundColorSpan(Color.GRAY), s.length() - (counter.length() + 2), s.length(), 0);
                    sColored.setSpan(new ForegroundColorSpan(Color.WHITE), s.length() - (counter.length() + 2), s.length(), 0);

                    element.setTitle(sColored);
                } else {
                    element.setTitle(before);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "setMenuCounterException " + e);
        }
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_CARRIAGE
                || getState() == STATE_TAKE_ORDER_FROM_STREET || getState() == STATE_CARRIAGE_CLIENT_FROM_STREET ) {
                if ( currentFragment != FRAGMENT_TAXOMETER )
                    replaceFragment(taxometerFragment);
                // taxometerFragment.onBackPressed();
            } else {
                boolean b = true;
                if ( currentFragment == FRAGMENT_ORDER_LIST )
                    b = ordersListFragment.onBackPressed();
                else if ( currentFragment == FRAGMENT_TAXOMETER ) {
                    b = taxometerFragment.onBackPressed();
                    if ( b ) {
                        replaceFragment(ordersListFragment);
                        b = false;
                    }
                }
                else if ( currentFragment == FRAGMENT_CHAT ) {
                    b = chatFragment.onBackPressed();
                    if ( b ) {
                        replaceFragment(ordersListFragment);
                        b = false;
                    }
                }
                else if ( currentFragment == FRAGMENT_CLIENT_ORDER ) {
                    b = clientOrderFragment.onBackPressed();
                    if ( b ) {
                        replaceFragment(ordersListFragment);
                        b = false;
                    }
                }
                else if ( currentFragment == FRAGMENT_MAP ) {
                    b = mapFragment.onBackPressed();
                    if ( b ) {
                        replaceFragment(ordersListFragment);
                        b = false;
                    }
                }
                else if ( currentFragment == FRAGMENT_NEWS ) {
                    b = newsFragment.onBackPressed();
                    if ( b ) {
                        replaceFragment(ordersListFragment);
                        b = false;
                    }
                }
                else if ( currentFragment == FRAGMENT_SOS ) {
                    b = false;
                    Prefs.putBoolean(TaxiActivity.this, Prefs.IS_SOS, true);
                    startActivity(new Intent(TaxiActivity.this, TaxiActivity.class));
                }
                if ( b ) super.onBackPressed();
            }
        }
    }

    public void stopRecord() {
        try {
            if ( recorder != null ) {
                recorder.stop();
                recorder.release();
                recorder = null;
                //ServerConnectService.talkieWalkie();
            }
        } catch (Exception e) {
            Log.e("mytaxieception", "recordExc " + e);
        }
    }

    public void record() {
        try {
            File file;
            String outputFile;
            if ( !audio_message ) {
                file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/lastSendTalkieWalkieRecordFile.mp3");
                outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/lastSendTalkieWalkieRecordFile.mp3";
            } else {
                file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/Messages/Audio/lastSendAudioRecordFile.mp3");
                outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/Messages/Audio/lastSendAudioRecordFile.mp3";
            }
            if ( file.exists() ) {
                file.delete();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            recorder.setOutputFile(outputFile);
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e("mytaxiexc", "ExcOnRec " + e);
        }
    }

    public int getState() {
        return Prefs.getIntDef(TaxiActivity.this, Prefs.APP_STATE, STATE_OFF);
    }

    @Override
    public void tryConnectToOrder(final int id) {
        String url = URL_TRY_GET_ORDER;

        final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    s = s.replace(" ", "");
                    Log.e(LOG_TAG, "get order request " + s);
                    if ( s.equals("0") ) {
                        request(0, 0);
                        if ( id != 0 )
                            Prefs.putInt(TaxiActivity.this, Prefs.APP_STATE, STATE_SYNCED_TO_ORDER);
                        else Prefs.putInt(TaxiActivity.this, Prefs.APP_STATE, STATE_TAKE_ORDER_FROM_STREET);
                        Prefs.putInt(TaxiActivity.this, Prefs.ORDER_ID, id);
                        try {
                            Firebase.setAndroidContext(TaxiActivity.this);
                            new Firebase("https://dos-f9894.firebaseio.com/order")
                                    .child("state").setValue(System.currentTimeMillis());
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }
                        request(2, 0);
                        // currentOrderInfoDialogShow();
                    } else if ( s.equals("-2") ) {
                        Toast.makeText(TaxiActivity.this, "Этот заказ уже неактивен!", Toast.LENGTH_SHORT).show();
                    } else if ( s.equals("-4") ) {
                        Toast.makeText(TaxiActivity.this, "Ошибка сервера!", Toast.LENGTH_SHORT).show();
                    } else if ( s.equals("-3") ) {
                        Toast.makeText(TaxiActivity.this, "Этот заказ уже неактивен!", Toast.LENGTH_SHORT).show();
                    } else if ( s.equals("-5") ) {
                        Toast.makeText(TaxiActivity.this, "Недостаточно средств на балансе!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("id", "" + id);
                params.put("driver", "" + Prefs.getIntDef(TaxiActivity.this, Prefs.DRIVER_ID, 0));
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    @Override
    public void curFrag(int f) {
        currentFragment = f;
        if ( f == FRAGMENT_TAXOMETER ) {
            buttonOrderStreet.setText("О заказе");
            buttonOrderStreet.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_info, 0, 0);
            if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_CARRIAGE )
                taxometerFragment.openOrCloseBottomSheet();
        }
    }

    @Override
    public void cancOrd() {
        request(3, 0);
    }

    @Override
    public void complOrd(final int w) {
        request(4, w);
    }

    @Override
    public void finishOrd() {
        replaceFragment(ordersListFragment);
    }

    @Override
    public void startWalking() {
        request(9, 0);
    }

    @Override
    public void openClientChat() {
        Bundle b = new Bundle();
        b.putInt("code", 5);
        chatFragment = ChatFragment.newInstance(this, b);
        chatFragment.show(getSupportFragmentManager(), "CHAT_FRAGMENT_TAG");
        // replaceFragment(chatFragment);
    }

    @Override
    public void dialogDismiss() {
        if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_CARRIAGE
                || getState() == STATE_TAKE_ORDER_FROM_STREET || getState() == STATE_CARRIAGE_CLIENT_FROM_STREET ) {
            if ( taxometerFragment != null && taxometerFragment.isVisible() ) {
                currentFragment = FRAGMENT_TAXOMETER;
            }
        } else {
            if ( ordersListFragment != null && ordersListFragment.isVisible() ) {
                currentFragment = FRAGMENT_ORDER_LIST;
            } else if ( mapFragment != null && mapFragment.isVisible() ) {
                currentFragment = FRAGMENT_MAP;
            } else if ( newsFragment != null && newsFragment.isVisible() ) {
                currentFragment = FRAGMENT_NEWS;
            } else if ( taxometerFragment != null && taxometerFragment.isVisible() ) {
                currentFragment = FRAGMENT_TAXOMETER;
            } else {
                currentFragment = FRAGMENT_SOS;
            }
        }
    }

    private void uploadFile(Uri file) {
        //checking if file is available
        if (file != null) {

            //getting the storage reference
            final String tempFile = "twf.mp3";

            StorageReference sRef = storageReference.child("uploads/" + tempFile);

            sRef.putFile(file)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.i(LOG_TAG, "onSuccess: " + taskSnapshot.getMetadata().getReference().getDownloadUrl().toString());
                            Firebase.setAndroidContext(TaxiActivity.this);
                            new Firebase("https://dos-f9894.firebaseio.com/tw")
                                    .child("file").setValue(tempFile);
                            new Firebase("https://dos-f9894.firebaseio.com/tw")
                                    .child("url").setValue("uploads/" + tempFile);
                            new Firebase("https://dos-f9894.firebaseio.com/tw")
                                    .child("code").setValue(System.currentTimeMillis());
                            Log.i(LOG_TAG, "Uploaded " + taskSnapshot.getUploadSessionUri().getPath().toString());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(TaxiActivity.this, "Ошибка! Не удалось отправить запись из рации!", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //displaying the upload progress
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            Log.e(LOG_TAG, "Uploaded " + ((int) progress) + "%...");
                        }
                    });
        } else {
            //display an error if no file is selected
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        CircularImageView bmImage;
        ImageView imageView;
        int code = 0;

        public DownloadImageTask(CircularImageView bmImage) {
            this.bmImage = bmImage;
            code = 1;
        }

        public DownloadImageTask(ImageView iv) {
            imageView = iv; code = 0;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if ( code == 0 && imageView != null ) {
                imageView.setImageBitmap(result);
            } else if ( code == 1 && bmImage != null ) {
                bmImage.setImageBitmap(result);
            }
        }
    }

    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if( mLastLocation != null ) {
            lat = mLastLocation.getLatitude();
        }
        return lat;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if( mLastLocation != null ) {
            lng = mLastLocation.getLongitude();
        }
        return lng;
    }

    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates()
    {
        locationUtils.togglePeriodicLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            mLastLocation = location;

            lat = location.getLatitude();
            lng = location.getLongitude();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        Log.e(LOG_TAG, "TaxiActivity location changed = " + lat + " - " + lng + "; speed = " + location.getSpeed());
    }

    @Override
    public void displayLocation(Location mLastLocation) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }




}
