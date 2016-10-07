/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Handle a suspended Google Play Services API connection
 */
abstract class AbstractGoogleConnectionCallbacks
        implements GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = AbstractGoogleConnectionCallbacks.class.getSimpleName();

    private final Context context;

    AbstractGoogleConnectionCallbacks(final @NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @UiThread
    @Override
    public void onConnectionSuspended(final int cause) {
        final String causeText;
        switch (cause) {
            case CAUSE_SERVICE_DISCONNECTED:
                causeText = "Google API service has been killed";
                break;
            case CAUSE_NETWORK_LOST:
                causeText = "Network connection lost";
                break;
            default:
                causeText = "Unidentified cause";
                break;
        }
        Log.i(TAG, "onConnectionSuspended(" + cause + "): " + causeText);

        final String appName = context.getString(R.string.app_name);
        Toast.makeText(context, appName + ": Google Play Services connection suspended",
                Toast.LENGTH_LONG).show();
    }
}
