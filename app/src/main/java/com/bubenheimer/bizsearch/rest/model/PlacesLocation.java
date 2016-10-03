/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest.model;

import android.support.annotation.Keep;

import com.google.api.client.util.Key;

/**
 * Places API JSON POJO for Google HTTP client parsing
 */
public final class PlacesLocation {
    @Key
    public Double lat;
    @Key
    public Double lng;

    @Keep
    public PlacesLocation() {
    }
}
