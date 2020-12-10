package kg.dos2.taxidriver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.qhutch.bottomsheetlayout.BottomSheetLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_TAXOMETER;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;
import static kg.dos2.taxidriver.TaxiActivity.STATE_CARRIAGE;
import static kg.dos2.taxidriver.TaxiActivity.STATE_CARRIAGE_CLIENT_FROM_STREET;
import static kg.dos2.taxidriver.TaxiActivity.STATE_OFF;
import static kg.dos2.taxidriver.TaxiActivity.STATE_SYNCED_TO_ORDER;
import static kg.dos2.taxidriver.TaxiActivity.STATE_TAKE_ORDER_FROM_STREET;
import static kg.dos2.taxidriver.TaxiActivity.tariffList;

public class TaxometerFragment extends Fragment implements OnMapReadyCallback, PermissionsListener,
        LocationUtils.MyLocationListener {

    private PermissionsManager permissionsManager;

    private TextView tax_mon, res_dist, tax_time, res_w_km, res_pay,
            res_w_land, res_w, tv_error, res_w_time, res_w_trf;
    private LinearLayout res_lay, tax_lay, map_lay;
    private Button res_bt_ok, bottom_bt;
    private BottomSheetLayout bottomSheet;

    private MapboxMap mMap;
    private MapView mv;

    private Location mLastLocation;

    private GPSTracker gps;
    private PolylineOptions line;

    private int a, m_value, currentTariff = 0;
    private float distanceTravelled, speed = 0;

    private Double lat_1, lon_1, L1, L2;

    private boolean timer_state = false,
            is_pause = false,
            is_price_set = false,
            is_waiting = false,
            flag = false;

    private Button act1, act2, setClientLoc;
    private TextView distanceDisplay;

    private FragmentsListener callBack;

    private int s = 0, m = 0, h = 0;
    private int waiting_m = 0;
    private Timer timer;

    private LocationUtils locationUtils;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public TaxometerFragment() {
        // Required empty public constructor
    }

    public static TaxometerFragment newInstance(Bundle args) {
        TaxometerFragment fragment = new TaxometerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        Mapbox.getInstance(getContext(), "pk.eyJ1IjoidGFhbGF5ZGV2IiwiYSI6ImNrM3JyemJqbzBkMnAzaHBzaTNtbWE0eDYifQ.6jS_pnpk66YaDc2EabZF4A");
    }

    @Override
    public void onResume() {
        super.onResume();
        is_pause = false;
        // Resuming the periodic location updates
        locationUtils.onResume();
        mv.onResume();
    }

    public void CheckGpsStatus(){
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        boolean GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if( GpsStatus ) {

        } else {
            Toast.makeText(getContext(), "Включите GPS", Toast.LENGTH_LONG).show();
            Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent1);
        }
    }

    public void startTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ( timer_state ) {
                                s++;
                                if (s == 60) {
                                    m++;
                                    s = 0;
                                    if ( is_waiting ) waiting_m++;
                                    if (m == 60) {
                                        m = 0;
                                        h++;
                                    }
                                }
                                if (h > 0) tax_time.setText("" + h + "h:" + m + "m");
                                else tax_time.setText("" + m + "m:" + s + "s");
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }
        }, 10, 1000L);
    }

    @Override
    public void onStart() {
        super.onStart();
        locationUtils.onStart();
        mv.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        locationUtils.onStop();
        mv.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        is_pause = true;
        mv.onPause();
        locationUtils.onPause();
    }

    int getTariffLand() {
        if ( !(currentTariff > tariffList.size()) )
            return tariffList.get(currentTariff).lnd;
        return 0;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mv.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mv.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mv.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_taxometer, container, false);

        locationUtils = new LocationUtils(getContext(), this);

        distanceTravelled = 0;
        currentTariff = Prefs.getIntDef(getContext(), Prefs.MY_TRF, 0);
        waiting_m = m_value = a = s = m = h = 0;
        is_pause = timer_state = is_price_set = is_waiting = false;
        lat_1 = lon_1 = L1 = L2 = 0d;

        if ( Order.current == null ) Order.current = new Order();
        if ( Order.current.getCost() != 0 && Order.current.getDist() != 0 ) {
            is_price_set = true;
            m_value = Order.current.getCost();
        }

        distanceDisplay = view.findViewById(R.id.taxom_km);
        tax_mon = view.findViewById(R.id.taxom_money);
        if ( is_price_set ) {
            distanceDisplay.setText(String.format(Locale.getDefault(), "%.2f км", Order.current.getDist()));
            tax_mon.setText(String.format(Locale.getDefault(), "%d с", Order.current.getCost()));
        } else {
            distanceDisplay.setText(String.format(Locale.getDefault(), "%d км", 0));
            tax_mon.setText(String.format(Locale.getDefault(), "%d с", getTariffLand()));
        }
        tax_time = view.findViewById(R.id.taxom_time);
        tax_time.setText("0:0");
        tv_error = view.findViewById(R.id.tv_error);
        tv_error.setVisibility(View.GONE);
        act1 = view.findViewById(R.id.act1);
        act2 = view.findViewById(R.id.act2);

        res_dist = view.findViewById(R.id.res_dist);
        res_w = view.findViewById(R.id.res_w);
        res_w_km = view.findViewById(R.id.res_w_km);
        res_w_land = view.findViewById(R.id.res_w_land);
        res_w_time = view.findViewById(R.id.res_waiting_time);
        res_w_trf = view.findViewById(R.id.res_waiting_trf);
        res_pay = view.findViewById(R.id.res_pay);

        res_bt_ok = view.findViewById(R.id.res_bt_ok);
        res_bt_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer_state = false;
                timer.cancel();
                callBack.finishOrd();
            }
        });

        setClientLoc = view.findViewById(R.id.setClientLoc);
        setClientLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( flag ) {
                    if ( Order.current.getLat2() != 0 && Order.current.getLng2() != 0 ) {
                        setClientLoc.setVisibility(View.GONE);
                        act1.setEnabled(true);
                        act2.setEnabled(true);
                        tv_error.setText("Error");
                        tv_error.setVisibility(View.GONE);
                        gps = new GPSTracker(getContext());
                        if (gps.canGetLocation()) {
                            mMap.clear();
                            L1 = gps.getLatitude();
                            L2 = gps.getLongitude();
                            loadGeoJson(L2, L1, Order.current.getLng2(), Order.current.getLat2());
                        }
                    } else {
                        Toast.makeText(getContext(), "Сначало укажите место посадки!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        bottom_bt = view.findViewById(R.id.bottom_bt);
        bottom_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_TAKE_ORDER_FROM_STREET ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Отказаться");
                    builder.setMessage("Вы действительно хотите отказаться от заказа?");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            callBack.cancOrd();
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

        res_lay = view.findViewById(R.id.res_lay);
        res_lay.setVisibility(View.GONE);
        tax_lay = view.findViewById(R.id.taksometer);
        tax_lay.setVisibility(View.VISIBLE);
        map_lay = view.findViewById(R.id.mapLayout);
        map_lay.setVisibility(View.VISIBLE);

        bottomSheet = view.findViewById(R.id.bottom_sheet_lay);
        bottomSheet.collapse();

        TextView tvaddr1 = view.findViewById(R.id.tvaddr1);
        tvaddr1.setText(Order.current.getAddr1());

        TextView tvaddr2 = view.findViewById(R.id.tvaddr2);
        tvaddr2.setText(Order.current.getAddr2());

        if ( Order.current.getDesc().isEmpty() ) {
            view.findViewById(R.id.linearLayout3).setVisibility(View.GONE);
        } else {
            TextView tvdesc = view.findViewById(R.id.tvdesc);
            tvdesc.setText(Order.current.getDesc());
        }

        Button bt_call = view.findViewById(R.id.bt_taxi_call);
        bt_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = Order.current.getPhone();
                Uri call = Uri.parse("tel:" + number);
                Intent surf = new Intent(Intent.ACTION_DIAL, call);
                startActivity(surf);
            }
        });

        Button bt_chat = view.findViewById(R.id.bt_taxi_chat);
        if ( Order.current != null ) {
            if ( Order.current.getClientId() == 0 )
                bt_chat.setEnabled(false);
            else
                bt_chat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callBack.openClientChat();
                    }
                });
        }

        ImageView bottom_sheet_close = view.findViewById(R.id.bottom_sheet_close);
        bottom_sheet_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openOrCloseBottomSheet();
            }
        });

        timer = new Timer();

        mv = view.findViewById(R.id.mapBox);
        mv.onCreate(savedInstanceState);
        try
        {
            // initilizeMap();
            mv.getMapAsync(this);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "map init exception " + e.toString());
        }

        if ( getState() == STATE_CARRIAGE || getState() == STATE_CARRIAGE_CLIENT_FROM_STREET ) {
            act1.setText("Завершить");
            act1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_complete, 0, 0);
            act2.setText("Ожидание");
            act2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_wait, 0, 0);
            bottom_bt.setEnabled(false);
            startWalking(null);
            timer_state = true;
            startTimer();
        } else if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_TAKE_ORDER_FROM_STREET ) {
            act1.setText("Начать");
            act1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_play, 0, 0);
            act2.setText("Отмена");
            act2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_canc, 0, 0);
            bottom_bt.setEnabled(true);
        }

        act1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(LOG_TAG, "act1 clicked");
                onActButtonClicked(view);
            }
        });

        act2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onActButtonClicked(view);
            }
        });
        callBack.curFrag(FRAGMENT_TAXOMETER);

        checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, 6);
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, 7);
        CheckGpsStatus();

        return view;
    }

    public void openOrCloseBottomSheet() {
        if ( bottomSheet.isExpended() ) {
            bottomSheet.collapse();
        } else {
            bottomSheet.expand();
        }
    }

    public void checkPermission(String permission, int code) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getContext(),
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    permission)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{ permission },
                        code);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }

    public void onActButtonClicked(View view) {
        if ( view.getId() == R.id.act1 ) {
            if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_TAKE_ORDER_FROM_STREET ) {
                try {
                    act1.setText("Завершить");
                    act1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_complete, 0, 0);
                    act2.setText("Ожидание");
                    act2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_wait, 0, 0);
                    Prefs.putInt(getContext(), Prefs.APP_STATE, getState() + 1);
                    callBack.startWalking();
                    timer_state = true;
                    removeRouts();
                    gps = new GPSTracker(getContext());
                    if ( !is_price_set ) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Указать на карте место посадки?");
                        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                act1.setEnabled(false);
                                act2.setEnabled(false);
                                tv_error.setText("Нажмите на карту что бы указать место посадки и нажмите ок!");
                                tv_error.setVisibility(View.VISIBLE);
                                setClientLoc.setVisibility(View.VISIBLE);
                                flag = true;
                            }
                        });
                        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startWalking(view);
                                startTimer();
                                dialogInterface.cancel();
                            }
                        });
                        builder.show();
                    } else {
                        if ( Order.current.getLat2() != 0 && Order.current.getLng2() != 0 )
                            loadGeoJson(gps.getLongitude(), gps.getLatitude(), Order.current.getLng2(), Order.current.getLat2());
                        startWalking(view);
                        startTimer();
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                }
            } else if ( getState() == STATE_CARRIAGE || getState() == STATE_CARRIAGE_CLIENT_FROM_STREET ) {
                final View v = view;
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Завершить");
                builder.setMessage("Завершить заказ?");
                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startWalking(v);
                        if ( bottomSheet.isExpended() )
                            bottomSheet.collapse();
                        timer_state = false;
                        timer.cancel();
                        if ( !(currentTariff > tariffList.size()) ) {
                            int waiting_price = tariffList.get(currentTariff).wt * waiting_m;
                            m_value += waiting_price;
                            res_w_trf.setText(String.format(Locale.getDefault(), "%d с", tariffList.get(currentTariff).wt));
                            res_w_km.setText(String.format(Locale.getDefault(), "%s с", String.valueOf(tariffList.get(currentTariff).trf_km)));
                            res_w_land.setText(String.format(Locale.getDefault(), "%d с", tariffList.get(currentTariff).lnd));
                        }
                        tax_lay.setVisibility(View.GONE);
                        map_lay.setVisibility(View.GONE);
                        if ( !is_price_set ) {
                            res_dist.setText(String.format(Locale.getDefault(), "%s км", distanceTravelled));
                            Order.current.setDist(distanceTravelled);
                        }
                        else {
                            res_dist.setText(String.format(Locale.getDefault(), "%s км", Order.current.getDist()));
                        }

                        res_w.setText(String.format(Locale.getDefault(), "%d с", m_value));
                        res_w_time.setText(String.format(Locale.getDefault(), "%d мин", waiting_m));
                        if ( Order.current.getTp() != 0 )
                            res_pay.setText("Поп. баланса");
                        res_lay.setVisibility(View.VISIBLE);
                        Order.current.setCost(m_value);
                        Order.current.setWmin(waiting_m);
                        new DataBaseHelper(getContext()).newReport(Order.current, getState());
                        callBack.complOrd(m_value);
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
        } else if ( view.getId() == R.id.act2 ) {
            if ( getState() == STATE_SYNCED_TO_ORDER || getState() == STATE_TAKE_ORDER_FROM_STREET ) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Отказаться");
                builder.setMessage("Вы действительно хотите отказаться от заказа?");
                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callBack.cancOrd();
                    }
                });
                builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            } else if ( getState() == STATE_CARRIAGE || getState() == STATE_CARRIAGE_CLIENT_FROM_STREET ) {
                if ( !is_waiting ) {
                    act2.setText("Продолжить");
                    act2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_cont, 0, 0);
                    is_waiting = true;
                } else {
                    act2.setText("Ожидание");
                    act2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_wait, 0, 0);
                    is_waiting = false;
                }
                if ( !is_price_set )
                    startWalking(view);
            }
        }
    }

    public void startWalking(View v) {
        try {
            gps = new GPSTracker(getContext());
            if (gps.canGetLocation()) {
                double lat_1 = gps.getLatitude();
                double lon_1 = gps.getLongitude();
            } else {
                //       gps.showSettingsAlert();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        if (a == 0)
        {
            try {
                // Snackbar
                if ( v != null ) {
                    Snackbar snack = Snackbar.make(v, "Началось измерение пройденного пути.", Snackbar.LENGTH_LONG).setAction("Action", null);
                    ViewGroup group = (ViewGroup) snack.getView();
                    group.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.floroGreen));
                    snack.show();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }

            a = 1;
        }
        else if(a == 1)
        {
            try {
                // Snackbar
                if ( v != null ) {
                    Snackbar snack = Snackbar.make(v, "Завершение измерения пройденного расстояния", Snackbar.LENGTH_LONG).setAction("Action", null);
                    ViewGroup group = (ViewGroup) snack.getView();
                    group.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.floroGreen));
                    snack.show();
                }
            } catch ( Exception e ) {
                Log.e(LOG_TAG, e.toString());
            }
            a = 0;
        }

        try {
            // Plot First PolyLine
            //line = new PolylineOptions().add(new LatLng(lat_1, lon_1),
                    ///new LatLng(lat_1, lon_1)).width(13).color(getContext().getResources().getColor(R.color.floroGreen));
            //mMap.addPolyline(line);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        // Watchout for location updates
        togglePeriodicLocationUpdates();
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getContext())) {

            // Get an instance of the component
            LocationComponent locationComponent = mMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(getContext(), loadedMapStyle).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.GPS);
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        Log.e(LOG_TAG, "onMapReady");

        try {
            mMap = mapboxMap;
            // mMap.getUiSettings().setZoomControlsEnabled(true);
            mapboxMap.setStyle(Style.MAPBOX_STREETS,
                    new Style.OnStyleLoaded() {
                        @Override
                        public void onStyleLoaded(@NonNull Style style) {
                            enableLocationComponent(style);
                            try {
                                Layer mapText = style.getLayer("country-label");
                                mapText.setProperties(textField("{name_ru}"));
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "country set exception " + e.toString());
                            }
                        }
                    });
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            // mMap.getUiSettings().setMyLocationButtonEnabled(true);
            // mMap.setMyLocationEnabled(true);
            // mMap.getLocationComponent().setLocationComponentEnabled(true);

            mMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public boolean onMapClick(@NonNull LatLng point) {
                    // Convert LatLng coordinates to screen pixel and only query the rendered features.
                    /*final PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                    List<Feature> features = mapboxMap.queryRenderedFeatures(pointf);
                    if ( features.size() != 0 ) {
                        // Get the first feature within the list if one exist
                        Feature feature = features.get(0);
                        // Ensure the feature has properties defined
                        if (feature.properties() != null) {
                            for (Map.Entry<String, JsonElement> entry : feature.properties().entrySet()) {
                                // Log all the properties
                                Log.d(LOG_TAG, String.format("%s = %s", entry.getKey(), entry.getValue()));
                            }
                        }
                    }*/
                    try {
                        if ( flag ) {
                            gps = new GPSTracker(getContext());
                            if (gps.canGetLocation()) {
                                mMap.clear();
                                L1 = gps.getLatitude();
                                L2 = gps.getLongitude();
                                Order.current.setLat2((float) point.getLatitude());
                                Order.current.setLng2((float) point.getLongitude());
                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(new LatLng(point.getLatitude(), point.getLongitude()));
                                mMap.addMarker(markerOptions);
                                // loadGeoJson(L2, L1, point.getLongitude(), point.getLatitude());
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setMessage("Не удалось определить местоположение!");
                                builder.setPositiveButton("Использовать таксометр", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startWalking(null);
                                        startTimer();
                                        flag = false;
                                        setClientLoc.setVisibility(View.GONE);
                                        act1.setEnabled(true);
                                        act2.setEnabled(true);
                                        tv_error.setText("Error");
                                        tv_error.setVisibility(View.GONE);
                                    }
                                });
                                builder.show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        try {
            gps = new GPSTracker(getContext());
            if (gps.canGetLocation()) {
                L1 = gps.getLatitude();
                L2 = gps.getLongitude();
            } else {
                //       gps.showSettingsAlert();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        try {
            LatLng coordinate = new LatLng(L1, L2);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 17);
            mMap.animateCamera(yourLocation);

            float lat1 = Order.current.getLat1();
            float lng1 = Order.current.getLng1();
            if ( lat1 != 0 || lng1 != 0 ) {
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(lat1, lng1))
                        .title("Клиент");
                mMap.addMarker(markerOptions);

                if ( getState() != STATE_CARRIAGE ) {
                    // new LoadGeoJson(TaxometerFragment.this).execute();
                    loadGeoJson(coordinate.getLongitude(), coordinate.getLatitude(), lng1, lat1);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        // check if map is created successfully or not
        if (mMap == null)
        {
            Toast.makeText(getContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
        }
    }

    public void removeRouts() {
        mMap.getStyle().removeLayer("linelayer");
        mMap.getStyle().removeSource("line-source");
        mMap.clear();
    }

    private void drawLines(@NonNull FeatureCollection featureCollection) {
        if (mMap != null) {
            mMap.getStyle(style -> {
                if (featureCollection.features() != null) {
                    if (featureCollection.features().size() > 0) {
                        style.removeLayer("linelayer");
                        style.removeSource("line-source");
                        style.addSource(new GeoJsonSource("line-source", featureCollection));
                        // The layer properties for our line. This is where we make the line dotted, set the
                        // color, etc.
                        style.addLayer(new LineLayer("linelayer", "line-source")
                                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                        PropertyFactory.lineOpacity(.7f),
                                        PropertyFactory.lineWidth(7f),
                                        PropertyFactory.lineColor(Color.parseColor("#3bb2d0"))));
                    }
                }
            });
        }
    }

    public void loadGeoJson(double lat1, double lon1, double lat2, double lon2) {
        String url = "https://api.mapbox.com/directions/v5/mapbox/driving/"+lat1+","+lon1+";"+lat2+","+lon2+
                "?geometries=geojson&access_token=" +
                "pk.eyJ1IjoidGFhbGF5ZGV2IiwiYSI6ImNrM3JyemJqbzBkMnAzaHBzaTNtbWE0eDYifQ.6jS_pnpk66YaDc2EabZF4A";

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject obj = new JSONObject(s);
                    JSONArray arr = obj.getJSONArray("routes");
                    String tmp = "{" +
                            "\"type\": \"FeatureCollection\"," +
                            "\"features\": [{" +
                            "\"type\": \"Feature\"," +
                            "\"properties\": {" +
                            "\"name\": \"\"" +
                            "},\"geometry\": " +
                                arr.getJSONObject(0).getJSONObject("geometry") +
                            "}]" +
                            "}";
                    // Log.e(LOG_TAG, "loadGeoJson " + tmp);
                    drawLines(FeatureCollection.fromJson(tmp));
                    if ( flag ) {
                        flag = false;
                        Order.current.setDist((float) (arr.getJSONObject(0).getDouble("distance")/1000));
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Рассчитать автоматически расстояние и цену за поездку?");
                        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if ( !(currentTariff > tariffList.size()) ) {
                                    int price = (int) (Order.current.getDist() * tariffList.get(currentTariff).trf_km)
                                            + tariffList.get(currentTariff).lnd;
                                    Order.current.setCost(price);
                                    m_value = price;
                                    is_price_set = true;
                                    distanceDisplay.setText(String.format(Locale.getDefault(), "%.2f км", Order.current.getDist()));
                                    tax_mon.setText(String.format(Locale.getDefault(), "%d с", Order.current.getCost()));
                                }
                            }
                        });
                        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startWalking(null);
                                startTimer();
                                dialogInterface.cancel();
                            }
                        });
                        builder.show();
                    }
                    Log.e(LOG_TAG, "loadGeoJson distance " + arr.getJSONObject(0).getInt("distance"));
                } catch (JSONException je) {
                    Log.e("myimag", "json exception " + je);
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);

            }
        });
        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap()
    {
        try {
            // SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            // smf.getMapAsync(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, "map init exception " + e.toString());
        }
    }

    /**
     * Method to display the location on UI ************************************************************ F L A G ************
     * * */
    public void displayLocation(Location mLastLocation)
    {
        this.mLastLocation = mLastLocation;

        if (mLastLocation != null)
        {
            try {
                if ( !is_price_set ) {
                    double latitude = mLastLocation.getLatitude();
                    double longitude = mLastLocation.getLongitude();

                    String sTemp = String.format(Locale.getDefault(), "%.2f", distanceTravelled);

                    distanceDisplay.setText(String.format("%s км", sTemp));

                    int temp = (int) (distanceTravelled * getTariffKm());
                    m_value = temp + getTariffLand();
                    tax_mon.setText(String.format(Locale.getDefault(), "%d с", m_value));

                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
        else
        {
            try {
                tv_error.setVisibility(View.VISIBLE);
                tv_error.setText("(Не удалось получить местоположение. Убедитесь, что на устройстве включено местоположение)");
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates()
    {
        locationUtils.togglePeriodicLocationUpdates();
    }

    public int getTariffKm() {
        if ( !(currentTariff > tariffList.size()) )
            return tariffList.get(currentTariff).trf_km;
        return 0;
    }

    public int getState() {
        return Prefs.getIntDef(getContext(), Prefs.APP_STATE, STATE_OFF);
    }

    public boolean onBackPressed() {
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        Double lat_2 = 0d, lon_2 = 0d;
        try {
            mLastLocation = location;
            lat_2 = location.getLatitude();
            lon_2 = location.getLongitude();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        Log.e(LOG_TAG, "location changed = " + lat_2 + " - " + lon_2 + "; speed = " + location.getSpeed());
        //Toast.makeText(getApplicationContext(), "Location changed!" + lat_2 +"   "+lon_2, Toast.LENGTH_SHORT).show();

        try {
            // Plot new Polyline on map
            line = new PolylineOptions().add(new LatLng(lat_1, lon_1), new LatLng(lat_2, lon_2)).width(7).color(getContext().getResources().getColor(R.color.floroGreen));
            mMap.addPolyline(line);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        try {
            // Calculating distance between new location and previous location
            float[] results = new float[1];
            Location.distanceBetween(lat_1, lon_1, lat_2, lon_2, results);

            float dtemp = (results[0] / 1000);
            if ( !(dtemp > 0.20) )
                distanceTravelled = distanceTravelled + dtemp;

            speed = location.getSpeed();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        lat_1 = lat_2;
        lon_1 = lon_2;

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(getContext(), "Разрешение не предоставлено", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

}
