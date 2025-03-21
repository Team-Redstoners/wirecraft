package com.nqind.wirecraft;


// "POJO" data class to just store things
// Only used for sending Firmata input events (for now)
public class WireEvent {
    // ID of device (int)
    // Please don't try to hook up more than 2 billion devices
    public int deviceID;
    
    // Pins are ordered left-to-right, top-to-bottom
    public int pinNum;

    // State. True = high, false = low
    public boolean state;

    // In case we do analog or something
    public int extendedState;

    // Simple digital IO event constructor
    public WireEvent(int deviceID, int pinNum, boolean state) {
        this.deviceID = deviceID;
        this.pinNum = pinNum;
        this.state = state;
        this.extendedState = 0;
    }

    // Extended constructor
    public WireEvent(int deviceID, int pinNum, boolean state, int extendedState) {
        this.deviceID = deviceID;
        this.pinNum = pinNum;
        this.state = state;
        this.extendedState = extendedState;
    }
}
