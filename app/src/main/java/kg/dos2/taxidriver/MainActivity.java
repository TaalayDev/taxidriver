package kg.dos2.taxidriver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static kg.dos2.taxidriver.Prefs.URL_CHECK_LOGIN;
import static kg.dos2.taxidriver.Prefs.URL_DRIVER_INFO;

public class MainActivity extends AppCompatActivity {

    LinearLayout checkLay;

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
                    2);

        } else {
            // Permission has already been granted
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        checkLay = findViewById(R.id.check_lay);
        boolean islogin = Prefs.getBoolean(this, Prefs.IS_LOGIN);
        if ( islogin ) {
            final String phone = Prefs.getStringDef(MainActivity.this, Prefs.PHONE, "");
            final String password = Prefs.getStringDef(MainActivity.this, Prefs.PSW, "");
            Bundle bundle = new Bundle();
            bundle.putString("phone", phone);
            bundle.putString("password", password);
            request(0, bundle);
        }
    }

    public void login(View view) {
        view.setEnabled(false);
        final String phone = ((EditText)findViewById(R.id.username)).getText().toString();
        final String password = ((EditText)findViewById(R.id.password)).getText().toString();
        Bundle bundle = new Bundle();
        bundle.putString("phone", phone);
        bundle.putString("password", password);
        request(0, bundle);
    }

    public void request(final int code, Bundle bund) {
        findViewById(R.id.check_lay).setVisibility(View.VISIBLE);
        String url = URL_CHECK_LOGIN;
        if ( code != 0 ) url = URL_DRIVER_INFO;
        final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    Log.e("taxidriver", "LoginRequest " + s);
                    Button button = findViewById(R.id.loginButton);
                    if ( code == 0 ) {
                        findViewById(R.id.check_lay).setVisibility(View.GONE);
                        if (s.equals("-1")) {
                            Toast.makeText(MainActivity.this,
                                    "Неправильный логин или пароль!",
                                    Toast.LENGTH_SHORT).show();
                            button.setEnabled(true);
                        } else if ( s.equals("-2") ) {
                            button.setEnabled(true);
                        } else {
                            JSONObject jobj = new JSONObject(s);
                            int id = jobj.getInt("id");
                            int balance = jobj.getInt("bal");
                            int dpayd = jobj.getInt("dpayd");
                            Prefs.putInt(MainActivity.this, Prefs.BALANCE, balance);
                            Prefs.putInt(MainActivity.this, Prefs.DPAYD, dpayd);
                            request(id, bund);
                        }
                    } else {
                        button.setEnabled(true);
                        JSONArray arr = new JSONArray(s);
                        Log.e("myimag", "products size - " + arr.length());
                        for ( int i = 0; i < arr.length(); i++ ) {
                            JSONObject obj = arr.getJSONObject(i);
                            int state = obj.getInt("State");
                            if ( state != -1 ) {
                                Prefs.putInt(MainActivity.this, Prefs.DRIVER_ID, obj.getInt("ID"));
                                Prefs.putInt(MainActivity.this, Prefs.MY_TRF, obj.getInt("AutoCat"));
                                Prefs.putInt(MainActivity.this, Prefs.BALANCE, obj.getInt("Balance"));
                                Prefs.putInt(MainActivity.this, Prefs.CITY, obj.getInt("City"));
                                Prefs.putString(MainActivity.this, Prefs.DRIVER, obj.getString("Login"));
                                Prefs.putString(MainActivity.this, Prefs.PHONE, obj.getString("Phone"));
                                Prefs.putString(MainActivity.this, Prefs.FIRST_NAME, obj.getString("FirstName"));
                                Prefs.putString(MainActivity.this, Prefs.LAST_NAME, obj.getString("LastName"));
                                Prefs.putString(MainActivity.this, Prefs.IMAGE, obj.getString("DriverImage"));
                                Prefs.putBoolean(MainActivity.this, Prefs.IS_LOGIN, true);
                                startActivity(new Intent(MainActivity.this, TaxiActivity.class));
                                finish();
                            } else {
                                findViewById(R.id.check_lay).setVisibility(View.GONE);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
                findViewById(R.id.check_lay).setVisibility(View.GONE);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                if ( code == 0 ) {
                    params.put("phone", bund.getString("phone", ""));
                    params.put("pass", bund.getString("password", ""));
                } else {
                    params.put("id", "" + code);
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

}
