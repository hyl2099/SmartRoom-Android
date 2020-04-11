package com.upm.smartroom.state;

public class RoomTemperature {
    private float mLastTemperature;
    private float mLastPressure;

    public RoomTemperature(float mLastTemperature, float mLastPressure) {
        this.mLastTemperature = mLastTemperature;
        this.mLastPressure = mLastPressure;
    }

    public float getmLastTemperature() {
        return mLastTemperature;
    }

    public void setmLastTemperature(float mLastTemperature) {
        this.mLastTemperature = mLastTemperature;
    }

    public float getmLastPressure() {
        return mLastPressure;
    }

    public void setmLastPressure(float mLastPressure) {
        this.mLastPressure = mLastPressure;
    }
}
