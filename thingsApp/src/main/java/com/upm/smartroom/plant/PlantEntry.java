package com.upm.smartroom.plant;

import java.util.Map;

public class PlantEntry {
    float temperature;
    float humidity;
    float humidityNeed;
    Long timestamp;
    String image;
    Map<String, Float> annotations;


    public PlantEntry() {
    }

    public PlantEntry(float temperature, float humidity, float humidityNeed, Long timestamp, String image, Map<String, Float> annotations) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.humidityNeed = humidityNeed;
        this.timestamp = timestamp;
        this.image = image;
        this.annotations = annotations;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getHumidityNeed() {
        return humidityNeed;
    }

    public void setHumidityNeed(float humidityNeed) {
        this.humidityNeed = humidityNeed;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Map<String, Float> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, Float> annotations) {
        this.annotations = annotations;
    }
}
