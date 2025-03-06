package com.nqind.wirecraft.device;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.firmata4j.Pin.Mode;

// i promise this will become useful later

public interface WirecraftDevice {
    public void setEventQ(ConcurrentLinkedQueue<Object> eventQ);
    public boolean setMode(int pin, Mode mode);
    public boolean setOutputPin(int pin, boolean state);
}
