package com.adarshahd.indianrailinfo.donate.models.train;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ahd on 7/10/15.
 */
public class Train {
    @SerializedName("arrival_time")
    private String arrivalTime;

    @SerializedName("destination")
    private String destination;

    @SerializedName("train_number")
    private String trainNumber;

    @SerializedName("source")
    private String source;

    @SerializedName("train_name")
    private String trainName;

    @SerializedName("departure_time")
    private String departureTime;

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }
}
