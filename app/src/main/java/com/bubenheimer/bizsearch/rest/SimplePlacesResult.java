/*
 * Copyright (c) 2015-2016 Uli Bubenheimer. All rights reserved.
 */

package com.bubenheimer.bizsearch.rest;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Encapsulates simplified results from Places query
 */
public final class SimplePlacesResult implements Parcelable {
    /**
     * Attribution values
     */
    public String attributions;

    /**
     * Location values
     */
    public List<LatLng> locations;

    /**
     * Name values
     */
    public List<String> names;

    /**
     * Latitude from query
     */
    public double latitude;

    /**
     * Longitude from query
     */
    public double longitude;

    public SimplePlacesResult() {
    }

    private SimplePlacesResult(final Parcel in) {
        attributions = in.readString();
        locations = in.createTypedArrayList(LatLng.CREATOR);
        names = in.createStringArrayList();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(attributions);
        dest.writeTypedList(locations);
        dest.writeStringList(names);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SimplePlacesResult> CREATOR = new Creator<SimplePlacesResult>() {
        @Override
        public SimplePlacesResult createFromParcel(final Parcel in) {
            return new SimplePlacesResult(in);
        }

        @Override
        public SimplePlacesResult[] newArray(final int size) {
            return new SimplePlacesResult[size];
        }
    };
}
