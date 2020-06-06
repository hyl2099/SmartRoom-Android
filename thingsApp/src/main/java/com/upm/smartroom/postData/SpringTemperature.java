package com.upm.smartroom.postData;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class SpringTemperature {
    @SerializedName("id")
    @Expose
    private Long id;
    @SerializedName("temperatureIndoor")
    @Expose
    private Float temperatureIndoor;
    @SerializedName("humidityIndoor")
    @Expose
    private Float humidityIndoor;
    @SerializedName("temperatureOutdoor")
    @Expose
    private Float temperatureOutdoor;
    @SerializedName("humidityOutdoor")
    @Expose
    private Float humidityOutdoor;
    @SerializedName("time")
    @Expose
    private Date time;

    public SpringTemperature() {
    }

    public SpringTemperature(SpringTemperature t) {
        this.temperatureIndoor = t.temperatureIndoor;
        this.humidityIndoor = t.humidityIndoor;
        this.temperatureOutdoor = t.temperatureOutdoor;
        this.humidityOutdoor = t.humidityOutdoor;
        this.time = t.time;
    }

    public SpringTemperature(Float temperatureIndoor, Float humidityIndoor, Float temperatureOutdoor, Float humidityOutdoor, Date time) {
        this.temperatureIndoor = temperatureIndoor;
        this.humidityIndoor = humidityIndoor;
        this.temperatureOutdoor = temperatureOutdoor;
        this.humidityOutdoor = humidityOutdoor;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getTemperatureIndoor() {
        return temperatureIndoor;
    }

    public void setTemperatureIndoor(Float temperatureIndoor) {
        this.temperatureIndoor = temperatureIndoor;
    }

    public Float getHumidityIndoor() {
        return humidityIndoor;
    }

    public void setHumidityIndoor(Float humidityIndoor) {
        this.humidityIndoor = humidityIndoor;
    }

    public Float getTemperatureOutdoor() {
        return temperatureOutdoor;
    }

    public void setTemperatureOutdoor(Float temperatureOutdoor) {
        this.temperatureOutdoor = temperatureOutdoor;
    }

    public Float getHumidityOutdoor() {
        return humidityOutdoor;
    }

    public void setHumidityOutdoor(Float humidityOutdoor) {
        this.humidityOutdoor = humidityOutdoor;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "SpringTemperature{" +
                "temperatureIndoor=" + temperatureIndoor +
                ", humidityIndoor=" + humidityIndoor +
                ", temperatureOutdoor=" + temperatureOutdoor +
                ", humidityOutdoor=" + humidityOutdoor +
                ", time=" + time +
                '}';
    }
}
