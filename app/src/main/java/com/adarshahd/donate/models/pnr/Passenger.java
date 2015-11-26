package com.adarshahd.donate.models.pnr;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ahd on 3/10/15.
 */
public class Passenger {
    @SerializedName("booking_status")
    private String bookingStatus;

    @SerializedName("current_status")
    private String currentStatus;

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
}
