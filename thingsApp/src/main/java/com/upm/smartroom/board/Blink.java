package com.upm.smartroom.board;

import android.util.Log;

import com.google.android.things.pio.Gpio;

import java.io.IOException;

public class Blink extends Thread {
    private static final String TAG = Blink.class.getSimpleName();

    public boolean ledOn = false;

    public int blinkInterval_ms;

    public Gpio gpio;

    public Blink(Gpio mLedGpio, int blinkInterval_ms, boolean initOn) throws Exception {
        this.blinkInterval_ms = blinkInterval_ms;

        this.gpio = mLedGpio;
        this.ledOn = initOn;
        this.gpio.setDirection(initOn? Gpio.DIRECTION_OUT_INITIALLY_HIGH : Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }

    public void close() {
        try {
            gpio.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } finally {

        }
        gpio = null;
    }

    public void run() {
        while (gpio!=null) {
            ledOn = !ledOn;
            try {
                gpio.setValue(ledOn);
                Log.i(TAG, "Toggle LED ["+gpio.getName() + "] " + (ledOn? "On":"Off"));
            } catch (IOException e) {
                Log.i(TAG, e.getMessage(), e);
            }

            try {
                Thread.sleep(blinkInterval_ms);
            } catch (Exception e) {
                Log.i(TAG, e.getMessage(), e);
                break;
            }
        }
    }
}
