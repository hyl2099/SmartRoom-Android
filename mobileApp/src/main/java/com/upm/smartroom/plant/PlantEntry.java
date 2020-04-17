package com.upm.smartroom.plant;

public class PlantEntry {
    String temperature;
    String humidity;
    String humidityNeed;
    Long timestamp;
    String image;
    String name;


    public PlantEntry() {
    }

    public PlantEntry(String temperature, String humidity, String humidityNeed, Long timestamp, String image) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.humidityNeed = humidityNeed;
        this.timestamp = timestamp;
        this.image = image;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getHumidityNeed() {
        return humidityNeed;
    }

    public void setHumidityNeed(String humidityNeed) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
