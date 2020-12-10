package kg.dos2.taxidriver;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class LocationUtils implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    interface MyLocationListener {
        void onLocationChanged(Location location);
        void displayLocation(Location mLastLocation);
    }

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 2000; // 10 sec
    private static int FATEST_INTERVAL = 1000; // 5 sec
    private static int DISPLACEMENT = 2; // 10 meters

    private Context context;

    MyLocationListener myLocationListener;

    public void onResume() {
        checkPlayServices();
        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    public void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void onPause() {
        stopLocationUpdates();
    }

    public LocationUtils(Context context, MyLocationListener myLocationListener) {
        this.context = context;
        this.myLocationListener = myLocationListener;
        if (checkPlayServices())
        {
            buildGoogleApiClient();
            createLocationRequest();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    public void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text
            //startWalking.setText(getString(R.string.btn_stop_location_updates));
            /*startWalking.setBackgroundColor(getResources().getColor(R.color.floroGreen));
            startWalking.setTextColor(getResources().getColor(R.color.colorPrimary));
            startWalking.setText("STOP JOURNEY");*/

            mRequestingLocationUpdates = true;

            startLocationUpdates();
        } else {
            // Changing the button text
            //startWalking.setText(getString(R.string.btn_start_location_updates));

            /*startWalking.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            startWalking.setTextColor(getResources().getColor(R.color.floroGreen));
            startWalking.setText("START JOURNEY");*/

            mRequestingLocationUpdates = false;
            // Stopping the location updates
            stopLocationUpdates();
        }
    }

    void displayLocation() {
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        myLocationListener.displayLocation(mLastLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        myLocationListener.onLocationChanged(location);
        // Displaying the new location on UI
        displayLocation();
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates()
    {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest()
    {
        try {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setFastestInterval(FATEST_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient()
    {
        try {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices()
    {
        try {
            Activity activity = (Activity) context;
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                    GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                            PLAY_SERVICES_RESOLUTION_REQUEST).show();
                } else {
                    Toast.makeText(context,
                            "This device is not supported.", Toast.LENGTH_LONG)
                            .show();
                    activity.finish();
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        return true;
    }

    public boolean canGetLocation() {
        return mRequestingLocationUpdates;
    }

}
