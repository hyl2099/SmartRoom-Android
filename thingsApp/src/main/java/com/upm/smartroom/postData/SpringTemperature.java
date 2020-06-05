package com.upm.smartroom.postData;

import java.util.Date;

public class SpringTemperature {
    private float temperatureIndoor;
    private float humidityIndoor;
    private float temperatureOutdoor;
    private float humidityOutdoor;
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

    public SpringTemperature(float temperatureIndoor, float humidityIndoor, float temperatureOutdoor, float humidityOutdoor, Date time) {
        this.temperatureIndoor = temperatureIndoor;
        this.humidityIndoor = humidityIndoor;
        this.temperatureOutdoor = temperatureOutdoor;
        this.humidityOutdoor = humidityOutdoor;
        this.time = time;
    }

    public float getTemperatureIndoor() {
        return temperatureIndoor;
    }

    public void setTemperatureIndoor(float temperatureIndoor) {
        this.temperatureIndoor = temperatureIndoor;
    }

    public float getHumidityIndoor() {
        return humidityIndoor;
    }

    public void setHumidityIndoor(float humidityIndoor) {
        this.humidityIndoor = humidityIndoor;
    }

    public float getTemperatureOutdoor() {
        return temperatureOutdoor;
    }

    public void setTemperatureOutdoor(float temperatureOutdoor) {
        this.temperatureOutdoor = temperatureOutdoor;
    }

    public float getHumidityOutdoor() {
        return humidityOutdoor;
    }

    public void setHumidityOutdoor(float humidityOutdoor) {
        this.humidityOutdoor = humidityOutdoor;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
