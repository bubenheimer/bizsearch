/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Handle a Google Play Services API connection failure
 */
//TODO consider exponential backoff
abstract class StdGoogleConnectionFailedListener
        implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = StdGoogleConnectionFailedListener.class.getSimpleName();

    private final Context context;

    StdGoogleConnectionFailedListener(final Context context) {
        this.context = context;
    }

    @UiThread
    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        if (connectionResult.getErrorCode() == ConnectionResult.CANCELED) {
            final String errorMsg =
                    "Google APIs connection error resolution cancelled: " + connectionResult;
            Log.e(TAG, errorMsg);
        } else {
            final String errorMsg =
                    "Unresolvable Google APIs connection error: " + connectionResult;
            Log.e(TAG, errorMsg);
        }
        Toast.makeText(context, "Unresolvable Google APIs connection error", Toast.LENGTH_LONG)
                .show();
    }
}
