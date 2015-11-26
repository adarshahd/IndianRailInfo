package com.adarshahd.donate.models.train;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ahd on 7/10/15.
 */
public class Availability {
    @SerializedName("date")
    private String date;

    @SerializedName("status")
    private String status;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
