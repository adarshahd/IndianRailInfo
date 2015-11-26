package com.adarshahd.donate.models.train;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by ahd on 7/10/15.
 */
public class Trains {
    @SerializedName("trains")
    private List<Train> mTrains;

    public List<Train> getTrains() {
        return mTrains;
    }

    public void setTrains(List<Train> mTrains) {
        this.mTrains = mTrains;
    }
}
