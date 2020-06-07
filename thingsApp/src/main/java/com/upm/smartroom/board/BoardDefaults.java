package com.upm.smartroom.board;

import android.os.Build;

public class BoardDefaults {
//    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_IMX7D_PICO = "imx7d_pico";

    //使用中
    /**
     * Return the GPIO pin that the Button is connected on.
     */
    public static String getGPIOForCameraButton() {
        switch (Build.DEVICE) {
//            case DEVICE_RPI3:
//                return "BCM21";
            case DEVICE_IMX7D_PICO:
                return "GPIO2_IO05";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }
    //使用中
    public static String getGPIOForAlarmButton() {
        switch (Build.DEVICE) {
            case DEVICE_IMX7D_PICO:
                return "GPIO2_IO07";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    //使用中
    //return LED
    public static String getGPIOForLED() {
        return "GPIO6_IO14";
    }

    //使用中
    //return Buzzer
    public static String getGPIOForBuzzer() {
        return "GPIO6_IO15";
    }

    //TODO
    //Switch
    public static String getGPIOForSwitcher(){
        return "GPIO1_IO10";
    }
    //使用中
    //Sendor for movement
    public static String getGPIOForMovementSensor(){
        return "GPIO6_IO13";
    }


}
