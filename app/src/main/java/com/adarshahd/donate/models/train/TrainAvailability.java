package com.adarshahd.donate.models.train;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by ahd on 7/10/15.
 */
public class TrainAvailability {
    @SerializedName("availability")
    private List<Availability> trainAvailability;


    public List<Availability> getTrainAvailability() {
        return trainAvailability;
    }

    public void setTrainAvailability(List<Availability> trainAvailability) {
        this.trainAvailability = trainAvailability;
    }
}
