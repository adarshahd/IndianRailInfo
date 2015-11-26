package com.adarshahd.indianrailinfo.models.pnr;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ahd on 3/10/15.
 */
public class PnrStatus {
    @SerializedName("reserved_upto")
    private String reservedUpto;

    @SerializedName("from")
    private String from;

    @SerializedName("boarding_point")
    private String boardingPoint;

    @SerializedName("total_passengers")
    private Integer totalPassengers;

    @SerializedName("ticket_type")
    private String ticketType;

    @SerializedName("pnr")
    private String pnr;

    @SerializedName("charting_status")
    private String chartingStatus;

    @SerializedName("train_number")
    private String trainNumber;

    @SerializedName("to")
    private String to;

    @SerializedName("boarding_date")
    private String boardingDate;

    @SerializedName("train_name")
    private String trainName;

    @SerializedName("class")
    private String travelClass;

    @SerializedName("passenger_status")
    private Passenger [] passengers;

    public String getReservedUpto() {
        return reservedUpto;
    }

    public void setReservedUpto(String reservedUpto) {
        this.reservedUpto = reservedUpto;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBoardingPoint() {
        return boardingPoint;
    }

    public void setBoardingPoint(String boardingPoint) {
        this.boardingPoint = boardingPoint;
    }

    public Integer getTotalPassengers() {
        return totalPassengers;
    }

    public void setTotalPassengers(Integer totalPassengers) {
        this.totalPassengers = totalPassengers;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getChartingStatus() {
        return chartingStatus;
    }

    public void setChartingStatus(String chartingStatus) {
        this.chartingStatus = chartingStatus;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getBoardingDate() {
        return boardingDate;
    }

    public void setBoardingDate(String boardingDate) {
        this.boardingDate = boardingDate;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public String getTravelClass() {
        return travelClass;
    }

    public void setTravelClass(String travelClass) {
        this.travelClass = travelClass;
    }

    public Passenger[] getPassengers() {
        return passengers;
    }

    public void setPassengers(Passenger[] passengers) {
        this.passengers = passengers;
    }
}
