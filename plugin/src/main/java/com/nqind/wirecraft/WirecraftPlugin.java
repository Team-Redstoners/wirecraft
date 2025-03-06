package com.nqind.wirecraft;

import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.firmata4j.IODevice;
import org.firmata4j.IODeviceEventListener;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.firmata4j.Pin.Mode;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.transport.NetworkTransport;
import org.firmata4j.transport.SerialTransport;
import org.firmata4j.transport.TransportInterface;

public class WirecraftPlugin extends JavaPlugin implements Listener {

    IODevice device;
    FileConfiguration config = getConfig();;
    BukkitTask inputTask;
    ConcurrentLinkedQueue<Object> eventQ = new ConcurrentLinkedQueue<Object>();

    World mainWorld;
    private Logger log;

    @Override
    public void onEnable() {
        // Initialize events and logging
        Bukkit.getPluginManager().registerEvents(this, this);
        log = getLogger();

        // Get config options
        config.addDefault("printAllRedstoneEvents", false);
        config.addDefault("deviceIsNetwork", false);
        config.addDefault("devicePath", "/dev/ttyUSB0");
        config.options().copyDefaults(true);
        saveConfig();

        // Initialize Firmata device
        log.info("Initializing Firmata device...");
        TransportInterface fmtTransport;
        if(config.getBoolean("deviceIsNetwork")) {
            fmtTransport = new NetworkTransport(config.getString("devicePath"));
        }
        else {
            fmtTransport = new SerialTransport(config.getString("devicePath"));
        }
        device = new FirmataDevice(fmtTransport);
        try {
            device.start();
            device.ensureInitializationIsDone();

            // Set output pin (TESTING)
            testPin = device.getPin(0);
            testPin.setMode(Mode.OUTPUT);
            inputPin = device.getPin(2);
            inputPin.setMode(Mode.INPUT);

            mainWorld = getServer().getWorld("world");
            inputPin.addEventListener(new PinEventListener() {
                @Override
                public void onModeChange(IOEvent event) {
                    log.info("Pin mode changed: " + event.getPin().getMode().name());
                }
                @Override
                public void onValueChange(IOEvent event) {
                    log.log(Level.INFO, "Value change: " + event.getValue());
                    Material mat = Material.STONE;
                    if(event.getValue() == 1) {
                        mat = Material.REDSTONE_BLOCK;
                    }
                    // mainWorld.getBlockAt(5, 64, 5).setType(mat);
                    List<Object> eventData = new ArrayList<Object>();
                    int[] pos = {5, 64, 5};
                    eventData.add(pos);
                    eventData.add(mat);
                    eventQ.add(eventData);
                    log.log(Level.INFO, "block: " + mainWorld.getBlockAt(5, 64, 5).getType().name());
                }
            });
            device.addEventListener(new IODeviceEventListener() {
                @Override
                public void onStart(IOEvent event) {}
                @Override
                public void onStop(IOEvent event) {}
                @Override
                public void onMessageReceive(IOEvent event, String message) {
                    log.info("Recv message: " + message);
                }
                @Override
                public void onPinChange(IOEvent event) {
                    Pin pin = event.getPin();
                    log.info("device onPinChange, pin: " + pin.getIndex() + ", value: " + pin.getValue() + ", mode: " + pin.getMode().name());
                }
            });

            log.info("Wirecraft Plugin enabled!");

            // Start input tick task
            inputTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    if(true) {
                        if(!eventQ.isEmpty()) {
                            ArrayList<Object> event = (ArrayList<Object>)eventQ.remove();
                            int[] pos = (int[])event.get(0);
                            Material mat = (Material)event.get(1);
                            mainWorld.getBlockAt(pos[0], pos[1], pos[2]).setType(mat);
                        }
                    }
                }
            }, 0, 0);
            
        }
        catch(Exception e) {
            log.log(Level.SEVERE, "Failed to initialize Firmata device!", e);
            try {
                device.stop();
            }
            catch(IOException ex) {
                log.warning("Failed to stop Firmata device. Whatever, ignoring.");
            }
        }
    }

    @Override
    public void onDisable() {
        try {
            device.stop();
            inputTask.cancel();
        }
        catch(IOException e) {
            log.log(Level.WARNING, "Could not stop Firmata device.", e);
        }
    }

    @EventHandler
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
        Block block = event.getBlock();

        // Debugging info in chat
        // TODO: move to logger.debug?
        if(config.getBoolean("printAllRedstoneEvents")) {
            getServer().broadcast(Component.text(
                "Event on block at x,y,z ("
                + block.getX() + ","
                + block.getY() + ","
                + block.getZ() + ")"
            ));
            getServer().broadcast(Component.text(
                "isBlockPowered(): " + block.isBlockPowered()
                + ", isBlockIndirectlyPowered(): " + block.isBlockIndirectlyPowered()
            ));
            getServer().broadcast(Component.text(
                "New/old current: "
                + event.getNewCurrent() + ","
                + event.getOldCurrent()
            ));
        }

        // Test for block position
        if(block.getX() == 0 && block.getY() == 64 && block.getZ() == 0) {
            getServer().broadcast(Component.text(
                "Output block is "
                + (block.isBlockPowered() ? "ON" : "OFF")
            ));
            try {
                testPin.setValue(block.isBlockPowered() ? 1 : 0);
            }
            catch(IOException e) {
                log.log(Level.SEVERE, "Communication error to Firmata device!", e);
            }
        }
    }
}
