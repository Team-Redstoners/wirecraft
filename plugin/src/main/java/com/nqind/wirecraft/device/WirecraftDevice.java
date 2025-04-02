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
    public boolean setMode(int rsPin, Mode mode);
    public boolean setOutputPin(int rsPin, boolean state);
    public void acceptEvent(BlockRedstoneEvent event);
    public void stop();
    public void setLocation(int[] coords, CoordDirection direction);
    public int[] getLocation();
    public CoordDirection getDirection();
    public int getPinCount();
    public int firmataPinToRSPin(int firmataPin);
    public int rsPinToFirmataPin(int rsPin);
    public int[] getOuterCorner();

    // Returns -1 on failure
    public int coordToRSPin(int x, int y, int z);
}
