package com.nqind.wirecraft.device;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.block.BlockRedstoneEvent;
import org.firmata4j.IODeviceEventListener;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.transport.TransportInterface;

import com.google.common.collect.ImmutableBiMap;
import com.nqind.wirecraft.WireEvent;

public class S3DevKit implements WirecraftDevice {

    // Pin mapping. Gosh this is awful
    // RS pin -> Firmata pin
    // Negative indicates disabled/special pin
    public ImmutableBiMap<Integer, Integer> pinMapping = new ImmutableBiMap.Builder<Integer, Integer>()
        // Left side of pins
        .put(1,-1)
        .put(3,4)
        .put(5,5)
        .put(7,6)
        .put(9,7)
        .put(11,15)
        .put(13,16)
        .put(15,17)
        .put(17,18)
        .put(19,8)
        .put(21,19)
        .put(23,20)
        .put(25,3)
        .put(27,46)
        .put(29,9)
        .put(31,10)
        .put(33,11)
        .put(35,12)
        .put(37,-2)
        .put(39,-3)
        // Right side of pins
        .put(2,-4)
        .put(4,1)
        .put(6,2)
        .put(8,42)
        .put(10,41)
        .put(12,40)
        .put(14,39)
        .put(16,38)
        .put(18,37)
        .put(20,36)
        .put(22,35)
        .put(24,0)
        .put(26,45)
        .put(28,48)
        .put(30,47)
        .put(32,21)
        .put(34,14)
        .put(36,13)
        .put(38,-5)
        .put(40,-6)
        .build();
    // RS pin -> Firmata pin
    // Hopefully it doesn't crash with all the -1's in there
    public ImmutableBiMap<Integer, Integer> inversePinMapping = pinMapping.inverse();

    FirmataDevice device;
    ConcurrentLinkedQueue<WireEvent> eventQ;
    Logger log;
    int deviceID;
    boolean active = false;
    int[] baseCoords;
    CoordDirection coordDirection;

    public S3DevKit(int deviceID, TransportInterface port, ConcurrentLinkedQueue<WireEvent> eventQ) {
        this.deviceID = deviceID;
        log =  Logger.getLogger("S3DevKit-" + deviceID);
        // Create device and event queue
        device = new FirmataDevice(port);
        setEventQ(eventQ);

        // Initialize Firmata device
        try {
            device.start();
            device.ensureInitializationIsDone();
        }
        catch(Exception e) {
            log.log(Level.SEVERE, "Failed to initialize Firmata device!", e);
            try {
                device.stop();
            }
            catch(Exception ex) {
                log.log(Level.WARNING, "Failed to stop Firmata device post-crash. Ignoring...");
            }
            return;
        }

        // Set all pins to input
        for(Pin pin : device.getPins()) {
            try {
                // Unknown bug: ConfigurableFirmata initializes all pins as input
                // However, these inputs don't work. Set them to another mode first (like pullups)
                // if(pin.getSupportedModes().contains(Mode.INPUT)) {
                //     pin.setMode(Mode.INPUT);
                //     // log.info("Set pin " + pin.getIndex() + " to INPUT");
                // }
                // if(pin.getSupportedModes().contains(Mode.PULLUP)) {
                //     pin.setMode(Mode.PULLUP);
                // }
                
            }
            catch(Exception e) {
                log.log(Level.WARNING, "Failed setting firmata pin mode", e);
            }
        }
        try {
            device.getPin(0).setMode(Mode.PULLUP);
            device.getPin(0).setMode(Mode.INPUT);
        }
        catch(Exception e) {
            log.log(Level.WARNING, "Failed setting firmata pin mode", e);
        }
        // Yippee, hopefully we're ready now
        // Now register an event handler for the device
        device.addEventListener(new IODeviceEventListener() {
            @Override
            public void onStart(IOEvent event) {
                log.info("Start event listener for S3DevKit-" + deviceID);
            }
            @Override
            public void onStop(IOEvent event) {}
            @Override
            public void onMessageReceive(IOEvent event, String message) {
                log.log(Level.INFO, "Recv message from S3DevKit: " + message);
            }
            @Override
            public void onPinChange(IOEvent event) {
                Pin pin = event.getPin();
                log.log(Level.INFO, "device onPinChange, pin: " + pin.getIndex() + ", value: " + pin.getValue() + ", mode: " + pin.getMode().name());
                if(active) {
                    if(pin.getMode() == Mode.INPUT) {
                        WireEvent event2 = new WireEvent(deviceID, pin.getIndex(), pin.getValue() == 1);
                        eventQ.add(event2);
                    }
                }
            }
        });


        log.info("Finished constructing S3DevKit-" + deviceID);
    }

    public int getDeviceID() {
        return deviceID;
    }

    public void setEventQ(ConcurrentLinkedQueue<WireEvent> eventQ) {
        this.eventQ = eventQ;
    }

    public int firmataPinToRSPin(int firmataPin) {
        // This SHOULD ALWAYS RETURN something valid
        if(inversePinMapping.containsKey(firmataPin)) {
            return inversePinMapping.get(firmataPin);
        }
        log.warning("Attempted to convert invalid Firmata pin " + firmataPin + " to redstone pin!");
        return -1;
    }

    public int rsPinToFirmataPin(int rsPin) {
        // This is expected to return -1 if it's a special pin
        // We can return -1 if the pin mapping doesn't contain the RS pin number for whatever reason
        if(pinMapping.containsKey(rsPin)) {
            return pinMapping.get(rsPin);
        }
        return -1;
    }

    public void stop() {
        try {
            this.device.stop();
        }
        catch(Exception e) {
            log.log(Level.WARNING, "Error when stopping Firmata device #" + deviceID, e);
        }
    }

    public int getPinCount() {
        return device.getPinsCount();
    }

    public boolean setMode(int pin, Mode mode) {
        try {
            device.getPin(pin).setMode(mode);
            return true;
        }
        catch(Exception e) {
            log.log(Level.WARNING, "Failed setting pin mode " + pin + " to " + mode.toString(), e);
            return false;
        }
    }

    public boolean setOutputPin(int pin, boolean state) {
        Pin dPin = device.getPin(pin);
        if(dPin.getMode() == Mode.OUTPUT) {
            try {
                device.getPin(pin).setValue(state ? 1 : 0);
                return true;
            }
            catch(Exception e) {
                log.log(Level.WARNING, "Failed setting pin val " + pin + " to " + state, e);
            }
        }
        return false;
    }

    public void acceptEvent(BlockRedstoneEvent event) {
        // TODO convert event coords to pin number and set device output pin
        if(active) {

        }
        else {
            log.log(Level.WARNING, "Sent BlockRedstoneEvent to inactive Wirecraft device");
        }
    }

    public void setLocation(int[] coords, CoordDirection direction) {
        active = true;
        baseCoords = coords.clone();
        coordDirection = direction;
        // TODO place down blocks in real world
    }

    public int[] getLocation() {
        return baseCoords.clone();
    }

    public CoordDirection getDirection() {
        return coordDirection;
    }
}
