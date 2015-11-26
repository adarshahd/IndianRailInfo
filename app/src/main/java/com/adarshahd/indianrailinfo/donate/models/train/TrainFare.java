package com.adarshahd.indianrailinfo.donate.models.train;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ahd on 7/10/15.
 */
public class TrainFare {
    @SerializedName("total")
    private String total;

    @SerializedName("reservation")
    private String reservationCharge;

    @SerializedName("other")
    private String otherCharge;

    @SerializedName("superfast")
    private String superfaseCharge;

    @SerializedName("base_fare")
    private String baseFare;

    @SerializedName("service_tax")
    private String serviceTax;

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getReservationCharge() {
        return reservationCharge;
    }

    public void setReservationCharge(String reservationCharge) {
        this.reservationCharge = reservationCharge;
    }

    public String getOtherCharge() {
        return otherCharge;
    }

    public void setOtherCharge(String otherCharge) {
        this.otherCharge = otherCharge;
    }

    public String getSuperfaseCharge() {
        return superfaseCharge;
    }

    public void setSuperfaseCharge(String superfaseCharge) {
        this.superfaseCharge = superfaseCharge;
    }

    public String getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(String baseFare) {
        this.baseFare = baseFare;
    }

    public String getServiceTax() {
        return serviceTax;
    }

    public void setServiceTax(String serviceTax) {
        this.serviceTax = serviceTax;
    }
}
