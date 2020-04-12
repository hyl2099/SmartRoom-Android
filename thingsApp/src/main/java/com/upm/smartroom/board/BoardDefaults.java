package com.upm.smartroom.board;

import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class BoardDefaults {
    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_IMX7D_PICO = "imx7d_pico";

    /**
     * Return the GPIO pin that the Button is connected on.
     */
    public static String getGPIOForButton() {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return "BCM21";
            case DEVICE_IMX7D_PICO:
                return "GPIO2_IO05";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    public static String getGPIOForAlarmButton() {
        switch (Build.DEVICE) {
            case DEVICE_IMX7D_PICO:
                return "GPIO2_IO07";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    //return LED
    public static String getGPIOForLED() {
        return "GPIO6_IO14";
    }

    //return RBGLED
    public static List<String> getGPIOForRGBLED() {
        List<String> rgb = new ArrayList<>();
        rgb.add("GPIO2_IO01");//Red
        rgb.add("GPIO2_IO02");//Green
        rgb.add("GPIO2_IO00");//Blue
        return rgb;
    }

    //return LED
    public static String getGPIOForBuzzer() {
        return "GPIO6_IO15";
    }

    //
    public static String getGPIOForLocker(){
        return "GPIO2_IO03";
    }
    //
    public static String getGPIOForSwitcher(){
        return "GPIO1_IO10";
    }

    public static String getGPIOForMovementSensor(){
        return "GPIO6_IO13";
    }
}
