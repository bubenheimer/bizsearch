/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;

/**
 * Standard handler for Google Play Services API ResultCallbacks
 */
class StdResultCallback extends ResultCallbacks<Status> {
    private static final String TAG = StdResultCallback.class.getSimpleName();

    public StdResultCallback() {
    }

    @Override
    public void onSuccess(@NonNull final Status status) {
        Log.v(TAG, "onSuccess(" + status + ")");
    }

    @Override
    public void onFailure(@NonNull final Status status) {
        Log.e(TAG, "onFailure(" + status + ")");
    }
}

