package com.upm.smartroom;

interface MotionSensor {
    void startup();

    void shutdown();

    interface Listener {
        void onMovement();
    }
}
