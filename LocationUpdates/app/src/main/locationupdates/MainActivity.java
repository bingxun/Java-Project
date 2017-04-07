/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location.sample.locationupdates;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.w3c.dom.Text;

import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Getting Location Updates.
 *
 * Demonstrates how to use the Fused Location Provider API to get updates about a device's
 * location. The Fused Location Provider is part of the Google Play services location APIs.
 *
 * For a simpler example that shows the use of Google Play services to fetch the last known location
 * of a device, see
 * https://github.com/googlesamples/android-play-location/tree/master/BasicLocation.
 *
 * This sample uses Google Play services, but it does not require authentication. For a sample that
 * uses Google Play services for authentication, see
 * https://github.com/googlesamples/android-google-accounts/tree/master/QuickStart.
 */
public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener, GpsStatus.Listener {

    protected static final String TAG = "location-updates-sample";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    // UI Widgets.
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected TextView mLastUpdateTimeTextView;
    protected TextView mLatitudeTextView;
    protected TextView mLongitudeTextView;
    protected TextView mSatelliteTotalTextView;
    protected TextView mSatelliteUsedTextView;
    protected TextView mSignalAccuracy;
    protected TextView mPrn;


    // Labels.
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected String mLastUpdateTimeLabel;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    int totalSatellite;
    int totalSatelliteUsed;
    float signalAccuracy;
    String getAllPrn, removePrnBracket;
    private LocationManager mService;
    private GpsStatus mStatus;
    private SatelliteView satelliteView;
    DecimalFormat df = new DecimalFormat("0.00");
    LinkedList<String> savedSNR = new LinkedList<String>();
    LinkedList<String> savedSamples = new LinkedList<String>();
    int tempCountCV7 =0;
    int tempCountCV = 0;
    int tempCountCVAfter = 0;
    int countCV =0;
    int countCVAfter =0;
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mService.addGpsStatusListener(this);
        satelliteView = (SatelliteView) findViewById(R.id.SatelliteView);


        // Locate the UI widgets.
        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        mLatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        mLongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.last_update_time_text);
        mSatelliteTotalTextView = (TextView) findViewById(R.id.tvSatTotal);
        mSatelliteUsedTextView = (TextView) findViewById(R.id.tvSatUsed);
        mSignalAccuracy = (TextView) findViewById(R.id.tvAccuracy);
        mPrn = (TextView) findViewById(R.id.tvPRN);


        // Set labels.
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateUI() {
        mLatitudeTextView.setText(String.format("%s: %f", mLatitudeLabel,
                mCurrentLocation.getLatitude()));
        mLongitudeTextView.setText(String.format("%s: %f", mLongitudeLabel,
                mCurrentLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(String.format("%s: %s", mLastUpdateTimeLabel,
                mLastUpdateTime));
        mSatelliteTotalTextView.setText(Integer.toString(totalSatellite));
        mSatelliteUsedTextView.setText(Integer.toString(totalSatelliteUsed));
        mSignalAccuracy.setText(signalAccuracy + " (" + getGrade() + ")");
        StringBuilder sb = new StringBuilder();


        mPrn.setText(sb.append(removePrnBracket));


        mPrn.setText(removePrnBracket);


        //  System.out.println(getAllPrn);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {

        double myLatitude = location.getLatitude();
        double myLongitude = location.getLongitude();
        int hour;
        int minute;
        int seconds;
        double temp = 0;
        float totalSamples = 0;
        float total2df = 0;
        float mean;
        float samples = 0;
        int countSamples = 0;
        double variance;
        double stddev;
        double coefficientOfVariation = 0;
        Location crntLocation = new Location("crntlocation");
        crntLocation.setLatitude(myLatitude);
        crntLocation.setLongitude(myLongitude);

        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        if (location.hasAccuracy()) {
            signalAccuracy = location.getAccuracy();
        }

        mStatus = mService.getGpsStatus(null);
        Iterable<GpsSatellite> satellites = mStatus.getSatellites();
        Iterator<GpsSatellite> sat = satellites.iterator();
        List<String> detail = new LinkedList<String>();
        satelliteView.setList(satellites);
        String lSatellites = null;
        String filename = "SNR_PRN_VALUE.csv";
        Calendar c;
        while (sat.hasNext()) {
            GpsSatellite satellite = sat.next();
            lSatellites =
                    "\t" + "PRN: " + satellite.getPrn() + "\t" +
                            "SNR: " + satellite.getSnr() + "\n";

            if (satellite.getPrn() > 0 && satellite.getPrn() <= 32) {

                savedSamples.add(Float.toString(satellite.getSnr()));

                detail.add(lSatellites);

                getAllPrn = String.valueOf(detail);

                removePrnBracket = getAllPrn.replace("[", "").replace("]", "").replace(",", "");

                int iTempCountInView = 0;
                int iTempCountInUse = 0;
                float iTempCountSNR = 0;

                if (satellites != null) {
                    for (GpsSatellite gpsSatellite : satellites) {
                        iTempCountInView++;
                        if (gpsSatellite.usedInFix()) {
                            iTempCountInUse++;
                        }

                        totalSatellite = iTempCountInView;
                        totalSatelliteUsed = iTempCountInUse;
                    }
                    temp = 0;
                    totalSamples = totalSamples + iTempCountSNR;
                    total2df = Float.parseFloat(df.format(totalSamples));
                    savedSNR.add(Float.toString(total2df));
                    System.out.println("Count SNR" + total2df);
                    countSamples++;

                }
                try

                {
                    FileOutputStream out = openFileOutput(filename, Context.MODE_APPEND);


                    System.out.println("Total SNR " + total2df);
                    System.out.println("Total Count Samples " + countSamples);
                    mean = total2df / countSamples;
                    System.out.println("Mean " + mean);

                    for (int j = 0; j < savedSamples.size(); j++) {
                        samples = Float.parseFloat(savedSamples.get(j));
                        System.out.println("Check Samples" + samples);
                        double sqrtdifftomean = Double.parseDouble(df.format(Math.pow(samples - mean, 2)));
                        System.out.println("Square diff to mean " + sqrtdifftomean);
                        temp += sqrtdifftomean;
                    }
                    System.out.println("Temp " + temp);
                    variance = Double.parseDouble(df.format(temp / countSamples));
                    System.out.println("Variance " + variance);
                    stddev = Double.parseDouble(df.format(Math.sqrt(variance)));
                    System.out.println("Standard Deviation " + stddev);
                    coefficientOfVariation = Double.parseDouble(df.format((stddev / mean) * 100));
                    System.out.println("Coefficient Of Variation " + coefficientOfVariation + "%");
                    System.out.println("Store inside notepad " + satellite.getPrn() + "," + samples + ",");

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            else
            {
               System.out.println("Other satellite detected");
            }
        }

            savedSamples.clear();
            savedSNR.clear();


            try {

                c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR);
                minute = c.get(Calendar.MINUTE);
                seconds = c.get(Calendar.SECOND);
                FileOutputStream out = openFileOutput(filename, Context.MODE_APPEND);
                String strcov = coefficientOfVariation + "," + hour + ":" + minute + ":" + seconds + "\n";
                countCV = tempCountCV;

                if (countCV < 5) {
                    Toast.makeText(getApplicationContext(), "Your location is original!",
                            Toast.LENGTH_LONG).show();

                }
                else if(countCV >= 5 && (coefficientOfVariation >= 5 && coefficientOfVariation < 10))
                {
                    Toast.makeText(getApplicationContext(), "Warning: Your location might be fake!",
                            Toast.LENGTH_LONG).show();

                }

                else if(countCV >= 5 && (coefficientOfVariation <= 5 || coefficientOfVariation >= 10))
                {

                    tempCountCVAfter++;
                }



                System.out.println("Store inside notepad " + strcov);
                System.out.println("Count No. of CV " + countCV);
                out.write(strcov.getBytes());
                countCVAfter = tempCountCVAfter;
                System.out.println("After spoof count " + countCVAfter + " times");
                if (countCVAfter >= 10)
                {
                    Toast.makeText(getApplicationContext(), "Your location is back to original!",
                            Toast.LENGTH_LONG).show();
                }
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateUI();
        }




    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onGpsStatusChanged(int changeType) {

    }

    private String getGrade()
    {
        if(signalAccuracy <= 10)
        {
            return "Good";
        }
        else if(signalAccuracy <= 30)
        {
            return "Fair";
        }
        else if(signalAccuracy <= 100)
        {
            return "Good";
        }
        else
        {
            return "Worst";
        }
    }
}
