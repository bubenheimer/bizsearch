/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest.model;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

import java.util.Set;

/**
 * Represents URL information
 */
public final class NearbySearchUrl extends GenericUrl {
    /**
     * Base query URL
     */
    private static final String ENCODED_URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    /**
     * Web API key for Places query
     */
    @Key("key")
    private String apiKey;

    /**
     * Center location for query, format as in
     * https://developers.google.com/places/web-service/search
     */
    @Key
    private String location;

    /**
     * Result ranking as in https://developers.google.com/places/web-service/search
     */
    @Key
    private String rankby;

    /**
     * Type of places results from
     * https://developers.google.com/places/web-service/supported_types
     */
    @Key
    private String type;

    /** Save latitude for later retrieval */
    private double latitude;

    /** Save longitude for later retrieval */
    private double longitude;

    public NearbySearchUrl() {
        super(ENCODED_URL);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Web API key for Places query
     */
    public NearbySearchUrl setApiKey(final String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Center location for query
     */
    public NearbySearchUrl setLocation(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        this.location = Double.toString(latitude) + ',' + Double.toString(longitude);
        return this;
    }

    /**
     * @param type Place search type from
     *             https://developers.google.com/places/web-service/supported_types
     */
    public NearbySearchUrl setType(final String type) {
        this.type = type;
        return this;
    }

    /**
     * @param rankby prioritizes results as defined by
     *               https://developers.google.com/places/web-service/search
     */
    public NearbySearchUrl setRankby(final String rankby) {
        this.rankby = rankby;
        return this;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        // consistency check - don't check the optional "type" and "rankby" parameters
        if (apiKey == null || location == null) {
            throw new RuntimeException("Invalid parameters");
        }
        return super.entrySet();
    }
}
