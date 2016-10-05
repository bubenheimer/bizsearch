/*
 * Copyright (c) 2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bubenheimer.bizsearch.rest.QueryManager;
import com.bubenheimer.bizsearch.rest.SimplePlacesResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

//TODO tests should be added, but this project is too much work for its purpose already
//TODO Comply better with Places API Terms of Use & Privacy Policy: https://developers.google.com/places/web-service/policies
/**
 * Displays a Google Map centered on the user's initial location. Queries for the closest florists
 * and displays locations on the map with info dialogs. Requeries as the user moves around.<p>
 *
 * Avoids requerying on every location change as users generally change their locations gradually,
 * so results would not change dramatically when moving in only a limited area.
 * This reduces network traffic, battery usage, and quota exhaustion.
 */
public final class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    /** Key for saving last query result */
    private static final String STATE_KEY_PLACES_RESULT = "placesResult";
    /** Key for saving whether map has ever been centered on a real location */
    private static final String STATE_KEY_CENTERED = "mapCentered";

    /** Marshmallow permissions request ID */
    private static final int PERMISSIONS_REQUEST = 1;

    /**
     * Update results every 10 seconds but only if there has been significant movement.
     * No faster updates required as it is not a navigation app.
     */
    private static final long LOCATION_UPDATE_INTERVAL = 10_000L;
    /** Smallest allowed location and query results update interval */
    private static final long LOCATION_UPDATE_FASTEST_INTERVAL = 1_000L;
    /** Required minimum distance between location updates in meters */
    private static final float LOCATION_UPDATE_SMALLEST_DISPLACEMENT = 30f;
    /** Distance threshold before allowing another Places API query */
    private static final int MIN_PLACES_QUERY_DISTANCE_METERS = 500;

    private GoogleApiClient googleApiClient;

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks;
    private GoogleApiClient.OnConnectionFailedListener connectionFailedHandler;

    private GoogleMap map;

    /** Places API query manager */
    private QueryManager queryManager;

    /**
     * Indicates whether the map has ever been centered on a real location as opposed to an old one
     */
    private boolean centeredOnRealLocation;

    /**
     * Caches most recent result from Places query
     */
    private SimplePlacesResult placesResultCache;

    /**
     * Array to hold distance computation results, to avoid allocating a new object every time.
     * Must be used from only the UI thread.
     */
    private final float[] distanceResults = new float[1];

    /** Handles query results */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @UiThread
        @Override
        public void onReceive(final Context context, final Intent intent) {
            switch (intent.getAction()) {
                case QueryManager.INTENT_ACTION_QUERY_SUCCESS:
                    placesResultCache = intent.getParcelableExtra(
                            QueryManager.INTENT_QUERY_SUCCESS_KEY_RESULT);
                    processCachedResult();
                    break;
                case QueryManager.INTENT_ACTION_QUERY_FAILURE:
                    final String message = intent.getStringExtra(
                            QueryManager.INTENT_QUERY_FAILURE_KEY_MSG);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initiate Marshmallow permissions check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSIONS_REQUEST);
        }

        connectionCallbacks = new ConnectionCallbacksHandler(this);
        connectionFailedHandler = new ConnectionFailedHandler(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, connectionFailedHandler)
                .addConnectionCallbacks(connectionCallbacks)
                .addApi(LocationServices.API)
                .build();

        setContentView(R.layout.activity_main);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_map);
        mapFragment.getMapAsync(this);

        queryManager = new QueryManager(this);

        if (savedInstanceState != null) {
            placesResultCache = savedInstanceState.getParcelable(STATE_KEY_PLACES_RESULT);
            centeredOnRealLocation = savedInstanceState.getBoolean(STATE_KEY_CENTERED);
        }

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(QueryManager.INTENT_ACTION_QUERY_SUCCESS);
        intentFilter.addAction(QueryManager.INTENT_ACTION_QUERY_FAILURE);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    /** Clean up and avoid memory leaks */
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        queryManager.cancelLast();
        queryManager = null;

        googleApiClient.unregisterConnectionCallbacks(connectionCallbacks);
        googleApiClient = null;

        connectionCallbacks = null;
        connectionFailedHandler = null;

        map = null;

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient.isConnected()) {
            FusedLocationApi.removeLocationUpdates(googleApiClient, this)
                    .setResultCallback(new StdResultCallback());
        }

        googleApiClient.disconnect();

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(STATE_KEY_PLACES_RESULT, placesResultCache);
        outState.putBoolean(STATE_KEY_CENTERED, centeredOnRealLocation);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_legal) {
            startActivity(new Intent(this, LegalActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        map = googleMap;
        setMyLocationEnabled();
        centerCamera();
        processCachedResult();
    }

    /**
     * Update Maps MyLocation functionality whenever map becomes available or permissions changed
     */
    private void setMyLocationEnabled() {
        if (map != null) {
            //noinspection MissingPermission
            map.setMyLocationEnabled(hasLocationPermission());
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissionDenied(grantResults)) {
            //Retries would be more user-friendly, but too much work in this case
            Toast.makeText(this, R.string.finish_no_permissions, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setMyLocationEnabled();
        requestLocationUpdates();
    }

    private boolean permissionDenied(final @NonNull int[] grantResults) {
        final int length = grantResults.length;

        if (length == 0) {
            return true;
        }

        for (final int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return true;
            }
        }

        return false;
    }

    private void requestLocationUpdates() {
        if (!googleApiClient.isConnected() || !hasLocationPermission()) {
            return;
        }

        // Code below could be executed more than once, ensure that won't cause problems

        final LocationRequest locationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(LOCATION_UPDATE_SMALLEST_DISPLACEMENT);
        //noinspection MissingPermission - we checked permissions above
        FusedLocationApi.requestLocationUpdates(googleApiClient,
                locationRequest, MainActivity.this)
                .setResultCallback(new LocationUpdateRequestResultCallback());
    }

    /**
     * Update camera once map, Google APIs, and permissions become available.
     */
    private void centerCamera() {
        if (map == null || !googleApiClient.isConnected() || !hasLocationPermission()) {
            return;
        }

        // Code below could be called more than once, ensure that won't cause problems

        // no location cache - getLastLocation() is cheap and often returns an ok location on start
        //noinspection MissingPermission - we checked permissions above
        final Location loc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (loc != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
                    new LatLng(loc.getLatitude(), loc.getLongitude()), 15f)));
        }
    }

    //If pre-M then we expect to have permissions already and won't call missing API
    @SuppressLint("NewApi")
    private boolean hasLocationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(final Location location) {
        scheduleQuery(location);

        //The initial last location is often bad - force centering the map on a real location once
        if (!centeredOnRealLocation) {
            centerCamera();
            //May not actually be centered yet if map still initializing, but map will trigger it
            centeredOnRealLocation = true;
        }
    }

    private void scheduleQuery(final Location location) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();

        //Run a Places query only in case of significant movement, unless we get re-created
        if (placesResultCache != null) {
            Location.distanceBetween(placesResultCache.latitude, placesResultCache.longitude,
                    latitude, longitude, distanceResults);
            if (Math.round(distanceResults[0]) < MIN_PLACES_QUERY_DISTANCE_METERS) {
                //Skip new query because user has not moved enough
                return;
            }
        }

        try {
            queryManager.schedulePlacesQuery(latitude, longitude);
        } catch (final IOException e) {
            Log.e(TAG, "Places search request scheduling error", e);
            queryFail();
        }
    }

    public void queryFail() {
        Toast.makeText(this, R.string.search_error, Toast.LENGTH_LONG).show();
    }

    @UiThread
    private void processCachedResult() {
        //Also covers the case of the Activity having been destroyed
        if (map == null || placesResultCache == null) {
            return;
        }

        //Clear all markers
        map.clear();

        final int size = placesResultCache.locations.size();
        for (int i = 0; i < size; i++) {
            map.addMarker(new MarkerOptions()
                    .position(placesResultCache.locations.get(i))
                    .title(placesResultCache.names.get(i))
                    .snippet(placesResultCache.attributions));
        }
    }

    private final class ConnectionCallbacksHandler extends AbstractGoogleConnectionCallbacks {
        ConnectionCallbacksHandler(final Context context) {
            super(context);
        }

        @Override
        public void onConnected(final Bundle bundle) {
            requestLocationUpdates();
        }
    }

    private final class ConnectionFailedHandler extends StdGoogleConnectionFailedListener {
        ConnectionFailedHandler(final Context context) {
            super(context);
        }

        @Override
        @UiThread
        public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
            super.onConnectionFailed(connectionResult);
            finish();
        }
    }

    private final class LocationUpdateRequestResultCallback extends StdResultCallback {
        @Override
        public void onFailure(@NonNull final Status status) {
            super.onFailure(status);
            Toast.makeText(
                    MainActivity.this, R.string.location_update_request_failed, Toast.LENGTH_SHORT)
                    .show();
            //Error would seem irrecoverable, so exit
            finish();
        }
    }
}
