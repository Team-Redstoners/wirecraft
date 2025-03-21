package com.nqind.wirecraft.device;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.event.block.BlockRedstoneEvent;
import org.firmata4j.Pin.Mode;

import com.nqind.wirecraft.WireEvent;

// i promise this will become useful later

public interface WirecraftDevice {
    public enum CoordDirection {
        EAST,
        NORTH,
        WEST,
        SOUTH;
    }
    public int getDeviceID();
    public void setEventQ(ConcurrentLinkedQueue<WireEvent> eventQ);
    public boolean setMode(int pin, Mode mode);
    public boolean setOutputPin(int pin, boolean state);
    public void acceptEvent(BlockRedstoneEvent event);
    public void stop();
    public void setLocation(int[] coords, CoordDirection direction);
    public int getPinCount();
    public int firmataPinToRSPin(int firmataPin);
    public int rsPinToFirmataPin(int rsPin);
}
