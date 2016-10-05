/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest.model.result;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Places API JSON POJO for Google HTTP client parsing.
 * See https://developers.google.com/places/web-service/search for details.
 */
public final class PlacesMetaResult {
    /**
     * Specifies who provided the information. Must be displayed to user with results.
     */
    @Key("html_attributions")
    public List<String> attributions;
    /**
     * Result status code. See https://developers.google.com/places/web-service/search for details.
     */
    @Key
    public String status;
    /**
     * Optional error message if result status is not "OK".
     * See https://developers.google.com/places/web-service/search for details.
     */
    @Key("error_message")
    public String errorMessage;
    /**
     * List of found places
     */
    @Key
    public List<PlacesResult> results;
}
