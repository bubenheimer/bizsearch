/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.ExponentialBackOff;

/**
 * Modifies the connection timeout, backoff strategy, and sets the JSON parser.
 */
class DefaultHttpRequestInitializer implements HttpRequestInitializer {
    private final JsonObjectParser jsonObjectParser =
            new JsonObjectParser(AndroidJsonFactory.getDefaultInstance());

    @Override
    public void initialize(final HttpRequest request) {
        //I have seen connection establishment from my home WiFi to Google App Engine take >20 secs,
        //so use that with some extra padding as a guideline for any Google API calls

        //Make sure to recreate these objects for every request

        //Use a shared backoff state for all response handlers.
        //Use defaults for exponential backoff, it's good enough.
        final ExponentialBackOff backOff = new ExponentialBackOff();
        final HttpBackOffIOExceptionHandler httpBackOffIOExceptionHandler =
                new HttpBackOffIOExceptionHandler(backOff);
        final HttpBackOffUnsuccessfulResponseHandler unsuccessfulResponseHandler =
                new HttpBackOffUnsuccessfulResponseHandler(backOff)
                        .setBackOffRequired(
                                HttpBackOffUnsuccessfulResponseHandler.BackOffRequired.ALWAYS);

        request
                .setConnectTimeout(30_000)
                .setNumberOfRetries(2)
                .setIOExceptionHandler(httpBackOffIOExceptionHandler)
                .setUnsuccessfulResponseHandler(unsuccessfulResponseHandler)
                .setParser(jsonObjectParser);
    }
}
