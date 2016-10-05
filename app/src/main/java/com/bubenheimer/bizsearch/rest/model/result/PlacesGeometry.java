/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest.model.result;

import com.google.api.client.util.Key;

/**
 * Places API JSON POJO for Google HTTP client parsing
 */
public final class PlacesGeometry {
    @Key
    public PlacesLocation location;
}
