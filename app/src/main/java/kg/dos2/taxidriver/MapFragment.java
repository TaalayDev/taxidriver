package kg.dos2.taxidriver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;
import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS;
import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS_BY_CATEGORY;
import static kg.dos2.taxidriver.Prefs.URL_GET_CATEGORIES;
import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_MAP;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class MapFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";

    private static final int REQUEST_CODE = 1000;

    String gpsCoord = "";
    double oldLat, oldLon, newLat, newLon, dist;
    boolean map2gisloaded = false;
    int st = 0;

    private WebView map;
    private FragmentsListener callBack;
    private MapView mapView;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public MapFragment() {
        // Required empty public constructor
    }

    private GoogleApiClient googleApiClient;

    public static MapFragment newInstance(double param1, double param2, int a) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_PARAM1, param1);
        args.putDouble(ARG_PARAM2, param2);
        args.putInt(ARG_PARAM3, a);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            newLat = getArguments().getDouble(ARG_PARAM1);
            newLon = getArguments().getDouble(ARG_PARAM2);
            st = getArguments().getInt(ARG_PARAM3);
        }
        Mapbox.getInstance(getContext(), "pk.eyJ1IjoidGFhbGF5ZGV2IiwiYSI6ImNrM3JyemJqbzBkMnAzaHBzaTNtbWE0eDYifQ.6jS_pnpk66YaDc2EabZF4A");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = view.findViewById(R.id.mapView);
        map.setVerticalScrollBarEnabled(false);
        map.getSettings().setJavaScriptEnabled(true);
        map.loadUrl("file:///android_asset/map_api.html");
        map.addJavascriptInterface(new MarkerWebInterface(getContext()), "android_callback");
        map.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String[] coord = gpsCoord.split(" ");
                if ( newLat == 0 && newLon == 0 ) {
                    newLat = 25.220317;
                    newLon = 55.232048;
                }
                String js = "initMap(" + newLat + ", " + newLon + ", 12);";

                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    map.evaluateJavascript(js, null);
                    map.evaluateJavascript("setMyMarker(" + newLat + ", " + newLon + ");", null);
                } else {
                    map.loadUrl("javascript: " + js);
                    map.loadUrl("javascript: setMyMarker(" + newLat + ", " + newLon + ");");
                }

                map2gisloaded = true;

                startTimer();
            }
        });
        if ( st != 1 ) callBack.curFrag(FRAGMENT_MAP);

        mapView = view.findViewById(R.id.mapBox);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        // Map is set up and the style has loaded. Now you can add data or make other map adjustments.

                    }
                });
                mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
                mapboxMap.getUiSettings().setCompassEnabled(true);
                mapboxMap.getUiSettings().setRotateGesturesEnabled(true);
            }
        });

        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(MapFragment.this)
                .addOnConnectionFailedListener(MapFragment.this)
                .addApi(LocationServices.API).build();

        return view;
    }

    public void startTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if ( getActivity() != null )
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Date d = new Date();
                        if (((d.getSeconds() % 5) == 0)) {
                            request(1);
                        }
                    }
                });
            }
        }, 10, 1000L);
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
                    Log.e(LOG_TAG, "loadGeoJson " + tmp);
                    // drawLines(FeatureCollection.fromJson(tmp));
                    if ( map2gisloaded ) {
                        String js = "loadGeoJson(" + tmp + ");";
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            map.evaluateJavascript(js, null);
                        } else {
                            map.loadUrl("javascript: " + js);
                        }
                    }
                } catch (JSONException je) {
                    Log.e("myimag", "loadgeojson json exception " + je);
                } catch (Exception e) {
                    Log.e("myimag", "loadGeoJSONException " + e);
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

    public void request(final int breakTime) {
        String url = URL_GET_ACTIVE_ORDERS;
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    JSONArray arr = new JSONArray(s);
                    Log.e(LOG_TAG, "products size - " + arr.length());
                    List<Marker> markers = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        if ( obj.getInt("Driver") == 0 ) {
                            // Order order = new Order();
                            /*order.setId(obj.getInt("ID"));
                            order.setState(obj.getInt("State"));
                            order.setTp(obj.getInt("tp"));
                            order.setAddr1(obj.getString("Addr1"));
                            order.setAddr2(obj.getString("Addr2"));
                            order.setDesc(obj.getString("Desc"));
                            order.setCat(obj.getInt("cat"));
                            order.setCost(obj.getInt("cost"));
                            order.setDist((float) obj.getDouble("dist"));*/
                            // addOrder(order);
                            markers.add(new Marker(String.valueOf(obj.getInt("ID")), obj.getDouble("lat1"), obj.getDouble("lng1")));
                        }
                    }

                    showMarkers(markers);
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
                if ( breakTime < 5 ) {
                    request(breakTime + 1);
                }
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("id", "" + 0);
                params.put("trf", "" + Prefs.getIntDef(getContext(), Prefs.MY_TRF, 0));
                params.put("city", "" + Prefs.getIntDef(getContext(), Prefs.CITY, 0));
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
        fusedLocationProviderApi.removeLocationUpdates(googleApiClient, new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                onClick(null);

            }
        });
    }

    public boolean onBackPressed() {
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();

        if(googleApiClient != null){
            googleApiClient.connect();
        }
    }

    public void setMarker(double newLat, double newLon) {
        String js = "moveMyMarker(" + newLat + ", " + newLon + ");";
        if (android.os.Build.VERSION.SDK_INT >= 19)
        {
            map.evaluateJavascript(js, null);
        }
        else {
            map.loadUrl("javascript: " + js);
        }
    }

    private void showMarkers(final List<Marker> markers)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= 19)
                {
                    map.evaluateJavascript(prepareMarkerMessage(markers), null);
                } else {
                    map.loadUrl("javascript: "+prepareMarkerMessage(markers));
                }
            }
        }, 600);
    }

    private String prepareMarkerMessage(List<Marker> markers)
    {
        return getMarkersArray(markers) + "var lat;var lon;setMarkersAndAvatar(markers, "+newLat+", "+newLon+", 'https://cdn-new.flamp.us/default-avatar-m_100_100.png');";
        //return getMarkersArray(markers) + "var lat; var lon; setMarkersAndAvatar(markers, null, null, null);";
    }

    public static StringBuilder getMarkersArray(List<Marker> markers)
    {
        final int optionsCount = 3;
        StringBuilder builder = new StringBuilder("var markers = new Array(" + markers.size() + ");");
        builder.append("for (var i = 0; i < " + markers.size() + "; i++) {markers[i] = new Array(" + optionsCount + ");} ");

        for (int i = 0; i < markers.size(); i++)
        {
            builder.append("markers[" + i + "][0] = " + markers.get(i).getLat() + ";");
            builder.append("markers[" + i + "][1] = " + markers.get(i).getLon() + ";");
            String[] ids = markers.get(i).getFilialIDs().substring(0, markers.get(i).getFilialIDs().length() - 1).split(",");
            builder.append("markers[" + i + "][2] = [");
            for (int j = 0; j < ids.length; j++)
            {
                builder.append("'" + ids[j] + "'");
                if (j + 1 < ids.length) builder.append(",");
            }
            builder.append("];");
        }
        return builder;
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "We are connected top the user location");

        FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setSmallestDisplacement(5);

        if (googleApiClient.isConnected()) {

            if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, new com.google.android.gms.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.e(LOG_TAG, "location changed " + location.getLatitude() + ", " + location.getLongitude());
                    newLat = location.getLatitude();
                    newLon = location.getLongitude();
                    if ( map2gisloaded && st != 1 ) {
                        String js = "moveMyMarker(" + location.getLatitude() + ", " + location.getLongitude() + ");";
                        if (android.os.Build.VERSION.SDK_INT >= 19) {
                            map.evaluateJavascript(js, null);
                        } else {
                            map.loadUrl("javascript: " + js);
                        }
                    }
                }
            });
        } else{

            googleApiClient.connect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "The connection failed");

        if(connectionResult.hasResolution()){

            try {
                connectionResult.startResolutionForResult(getActivity(), REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else{

            Toast.makeText(getContext(), "Google play services is not working. EXIT!", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE && requestCode == RESULT_OK){

            googleApiClient.connect();

        }
    }

    public class Marker
    {
        private String filial_id;
        private double lon;
        private double lat;
        private String filialIDs;

        public Marker(String filial_id, double lat, double lon)
        {
            this.filial_id = filial_id;
            this.lon = lon;
            this.lat = lat;
            this.filialIDs = filial_id;
        }

        public String getFilial_id() {
            return filial_id;
        }

        public void setFilial_id(String filial_id) {
            this.filial_id = filial_id;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public String getFilialIDs() {
            return filialIDs;
        }

        public void setFilialIDs(String filialIDs) {
            this.filialIDs = filialIDs;
        }
    }

    private class MarkerWebInterface
    {
        private Context context;

        MarkerWebInterface(Context c) {
            context = c;
        }

        @JavascriptInterface
        public void clickCopyright(final String url)
        {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }

        @JavascriptInterface
        public void touchMarker(final String filialIds, final String lat, final String lon, final boolean isActive)
        {
            Toast.makeText(context, "Filial id is "+filialIds,Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void onMapClicked(float lat, float lng)
        {
            loadGeoJson(newLat, newLon, lat, lng);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

}
