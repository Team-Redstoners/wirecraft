package com.nqind.wirecraft.device;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.transport.TransportInterface;

public class WirecraftS3R1 implements WirecraftDevice {
    FirmataDevice device;
    ConcurrentLinkedQueue<Object> eventQ;
    Logger log = Logger.getLogger("WirecraftS3R1");

    public WirecraftS3R1(TransportInterface port, ConcurrentLinkedQueue<Object> eventQ) {

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
        }

        // Set all pins to input
        for(Pin pin : device.getPins()) {
            try {
                pin.setMode(Mode.INPUT);
            }
            catch(Exception e) {
                log.log(Level.WARNING, "Failed setting firmata pin mode", e);
            }
        }
        // Yippee, hopefully we're ready now
    }

    public void setEventQ(ConcurrentLinkedQueue<Object> eventQ) {
        this.eventQ = eventQ;
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
                return false;
            }
        }
        else {
            return false;
        }
    }
}
