/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Displays licenses as required by terms of included APIs
 */
public final class LegalActivity extends AppCompatActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_legal);

        final TextView textView = (TextView) findViewById(android.R.id.message);
        textView.setHorizontallyScrolling(true);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setText(GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(this));
    }
}
