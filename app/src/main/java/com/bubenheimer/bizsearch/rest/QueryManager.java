/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bubenheimer.bizsearch.R;
import com.bubenheimer.bizsearch.rest.model.PlacesGeometry;
import com.bubenheimer.bizsearch.rest.model.PlacesLocation;
import com.bubenheimer.bizsearch.rest.model.PlacesMetaResult;
import com.bubenheimer.bizsearch.rest.model.PlacesResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//TODO The current approach limits search results to the 20 closest matches. It is possible to return more, but requires more work.

/**
 *
 * Implementation detail: uses the Google HTTP Client library.<p>
 *
 * Queries are scheduled via a Future from the library on a single execution thread,
 * guaranteeing serial execution. A response handler is schedule on the same thread right
 * after query execution. Cancelling a query Future causes an essentially no-op response handler
 * execution. Before a new query is scheduled, any old query is cancelled.
 */
public final class QueryManager {
    private static final String TAG = QueryManager.class.getSimpleName();

    /**
     * Intent for returning structured results. Called on successful Places search.
     */
    public static final String INTENT_ACTION_QUERY_SUCCESS =
            "com.bubenheimer.bizsearch.QUERY_SUCCESS";
    /**
     * Contains SimplePlacesResult
     */
    public static final String INTENT_QUERY_SUCCESS_KEY_RESULT = "result";

    /**
     * Intent indicating Places query failure.
     */
    public static final String INTENT_ACTION_QUERY_FAILURE =
            "com.bubenheimer.bizsearch.QUERY_FAILURE";
    /**
     * Contains error message String suitable for display to user
     */
    public static final String INTENT_QUERY_FAILURE_KEY_MSG = "message";

    /** Places search type */
    private static final String TYPE = "florist";

    /** Rank results by distance. */
    private static final String RANK_BY = "distance";

    /** Application context */
    private final Context context;

    private final HttpRequestFactory requestFactory;

    private final NearbySearchUrl nearbySearchUrl;

    /** A Future for the single currently pending or previous query, if any. Is set
     * whenever a new query is scheduled. A Future allows background execution with easy
     * cancellation.
     */
    private volatile Future<HttpResponse> httpResponseFuture;

    /**
     * Executor guaranteeing sequential query execution
     */
    private final Executor executor = Executors.newSingleThreadExecutor();

    public QueryManager(final @NonNull Context context) {
        this.context = context.getApplicationContext();

        final NetHttpTransport transport = new NetHttpTransport.Builder().build();
        requestFactory = transport.createRequestFactory(new DefaultHttpRequestInitializer());
        nearbySearchUrl = new NearbySearchUrl()
                .setApiKey(context.getString(R.string.google_api_key))
                .setRankby(RANK_BY)
                .setType(TYPE);
    }

    /**
     * Finds the 20 closest places for a given location
     */
    public void schedulePlacesQuery(final double latitude, final double longitude)
            throws IOException {
        cancelLast();

        nearbySearchUrl.setLocation(latitude, longitude);
        final HttpRequest request = requestFactory.buildGetRequest(nearbySearchUrl);
        httpResponseFuture = request.executeAsync(executor);
        //ResponseHandler to be called right after each HttpResponse completes
        executor.execute(new ResponseHandler(httpResponseFuture));
    }

    /**
     * Cancels the most recent http request, unless it is already done.
     */
    public void cancelLast() {
        if (httpResponseFuture != null && !httpResponseFuture.isDone()) {
            httpResponseFuture.cancel(true);
        }
    }

    //TODO would be nice to tie additional failures from the ResponseHandler into the exponential backoff pattern
    /**
     * ResponseHandler to be called right after each HttpResponse completes. Takes care of
     * parsing the resonse and broadcasting the relevant parts.
     */
    private final class ResponseHandler implements Runnable {
        /**
         * Local copy of associated response. Needed as a new query could be scheduled concurrently,
         * overwriting the global Future.
         */
        private final Future<HttpResponse> httpResponseFuture;

        private ResponseHandler(final Future<HttpResponse> httpResponseFuture) {
            this.httpResponseFuture = httpResponseFuture;
        }

        @Override
        public void run() {
            if (httpResponseFuture.isCancelled()) {
                //We cancelled the request, exit quickly
                return;
            }

            final HttpResponse httpResponse;
            try {
                //This really just gets the result, without blocking, as the response is complete
                //at this point.
                httpResponse = httpResponseFuture.get();
            } catch (final CancellationException e) {
                Log.wtf(TAG,
                        "Places search request not cancelled but throws CancellationException");
                fail();
                return;
            } catch (final InterruptedException e) {
                Log.wtf(TAG, "Places search request was complete but throws InterruptedException");
                fail();
                return;
            } catch (final ExecutionException e) {
                Log.e(TAG, "Places search request threw exception: " + e.getCause().getMessage());
                fail();
                return;
            }

            //Handle HTTP status code
            final int statusCode = httpResponse.getStatusCode();
            //The only valid response for us is OK, everything else is not usable or undefined
            if (statusCode != HttpStatusCodes.STATUS_CODE_OK) {
                Log.e(TAG, "Invalid status code returned: " + statusCode + ": "
                        + httpResponse.getStatusMessage());
                fail();
                return;
            }

            try {
                //Parse JSON result into hierarchy of model classes
                final PlacesMetaResult result = httpResponse.parseAs(PlacesMetaResult.class);
                //Check the status that is an element of the message. Distinct from HTTP status.
                switch (result.status) {
                    case "OK":
                    case "ZERO_RESULTS":
                        //Simplify the model class hierarchy for easy passing
                        final SimplePlacesResult simpleResult = simplifyResult(result);
                        if (simpleResult == null) {
                            Log.e(TAG, "Result simplification failed");
                            fail();
                        } else {
                            //Retrieve the user location from the original query
                            final NearbySearchUrl url =
                                    (NearbySearchUrl) httpResponse.getRequest().getUrl();
                            simpleResult.latitude = url.getLatitude();
                            simpleResult.longitude = url.getLongitude();
                            final Intent intent = new Intent(INTENT_ACTION_QUERY_SUCCESS)
                                    .putExtra(INTENT_QUERY_SUCCESS_KEY_RESULT, simpleResult);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                        break;

                    case "OVER_QUERY_LIMIT":
                    case "REQUEST_DENIED":
                    case "INVALID_REQUEST":
                    default:
                        //Bad errors that we cannot handle
                        Log.e(TAG, "Bad Places search result status: " + result.status);
                        fail();
                }
            } catch (final IOException e) {
                Log.e(TAG, "Error parsing Places search result", e);
                fail();
            }
        }

        /**
         * Broadcasts error message
         */
        private void fail() {
            final Intent intent = new Intent(INTENT_ACTION_QUERY_FAILURE)
                    .putExtra(INTENT_QUERY_FAILURE_KEY_MSG,
                            context.getString(R.string.search_error));
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

        /**
         * Simplify the model class hierarchy into a Parcelable object for easy passing
         * and storage.
         * @param result
         * @return
         */
        private SimplePlacesResult simplifyResult(final PlacesMetaResult result) {
            final List<PlacesResult> results = result.results;
            if (results == null) {
                return null;
            } else {
                final SimplePlacesResult simpleResult = new SimplePlacesResult();

                //Attributions are required to be displayed to user to comply with Terms
                final List<String> attributions = result.attributions;
                if (attributions != null && !attributions.isEmpty()) {
                    final StringBuilder builder = new StringBuilder("Attributions:");
                    for (final String attribution : attributions) {
                        builder.append('\n').append(attribution);
                    }
                    simpleResult.attributions = builder.toString();
                }

                simpleResult.locations = new ArrayList<>();
                simpleResult.names = new ArrayList<>();

                for (final PlacesResult placesResult : results) {
                    final String name = placesResult.name;
                    if (name != null && !Data.isNull(name)) {
                        final PlacesGeometry geometry = placesResult.geometry;
                        if (geometry != null) {
                            final PlacesLocation location = geometry.location;
                            if (location != null) {
                                final Double latitude = location.lat;
                                final Double longitude = location.lng;
                                if (latitude != null && !Data.isNull(latitude) &&
                                        longitude != null && !Data.isNull(longitude)) {
                                    simpleResult.locations.add(
                                            new LatLng(latitude, longitude));
                                    simpleResult.names.add(name);
                                }
                            }
                        }
                    }
                }

                return simpleResult;
            }
        }
    }
}

