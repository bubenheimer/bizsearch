/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Displays licenses as required by terms of included APIs
 */
public final class LegalActivity extends AppCompatActivity {
    private static final String TAG = LegalActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_legal);

        // Wait for first layout to show placeholder text,
        // then kick off inserting the real text in the background
        final TextView legalTextView = (TextView) findViewById(R.id.legal_text);
        final ViewTreeObserver viewTreeObserver = legalTextView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String licenseInfo = GoogleApiAvailability.getInstance()
                                .getOpenSourceSoftwareLicenseInfo(LegalActivity.this);
                        legalTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (licenseInfo == null) {
                                    Log.e(TAG, "Google APIs unavailable");
                                    Toast.makeText(LegalActivity.this, "Google APIs unavailable",
                                            Toast.LENGTH_SHORT).show();
                                    LegalActivity.this.finish();
                                    return;
                                }

                                legalTextView.setGravity(Gravity.NO_GRAVITY);
                                legalTextView.setHorizontallyScrolling(true);
                                legalTextView.setMovementMethod(new ScrollingMovementMethod());
                                legalTextView.setText(licenseInfo);
                            }
                        });
                    }
                }).start();
            }
        });
    }
}
