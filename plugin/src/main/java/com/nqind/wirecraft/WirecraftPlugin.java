package com.nqind.wirecraft;

import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.firmata4j.IODevice;
import org.firmata4j.IODeviceEventListener;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.firmata4j.Pin.Mode;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.transport.NetworkTransport;

public class WirecraftPlugin extends JavaPlugin implements Listener {

    IODevice device;
    FileConfiguration config = getConfig();
    Pin testPin;
    Pin inputPin;

    World mainWorld;
    private Logger log;

    @Override
    public void onEnable() {
        // Initialize events and logging
        Bukkit.getPluginManager().registerEvents(this, this);
        log = getLogger();

        // Get config options
        config.addDefault("printAllRedstoneEvents", false);
        config.addDefault("serialDevice", "/dev/ttyUSB0");
        config.options().copyDefaults(true);
        saveConfig();

        // Initialize Firmata device
        // device = new FirmataDevice(config.getString("serialDevice"));
        log.info("Initializing Firmata device...");
        device = new FirmataDevice(new NetworkTransport("localhost:8555"));
        try {
            device.start();
            device.ensureInitializationIsDone();

            // Set output pin (TESTING)
            testPin = device.getPin(0);
            testPin.setMode(Mode.OUTPUT);
            inputPin = device.getPin(2);
            

            mainWorld = getServer().getWorld("world");
            inputPin.addEventListener(new PinEventListener() {
                @Override
                public void onModeChange(IOEvent event) {
                    log.info("Pin mode changed: " + event.getPin().getMode().name());
                }
                @Override
                public void onValueChange(IOEvent event) {
                    log.log(Level.INFO, "Value change: " + event.getValue());
                    Material mat = Material.AIR;
                    if(event.getValue() == 0) {
                        mat = Material.REDSTONE_BLOCK;
                    }
                    // mainWorld.getBlockAt(5, 64, 5).setType(mat);
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
            inputPin.setMode(Mode.INPUT);
            inputPin.setMode(Mode.PULLUP);

            log.info("Wirecraft Plugin enabled!");
        }
        catch(Exception e) {
            log.log(Level.SEVERE, "Failed to initialize Firmata device!", e);
            try {
                device.stop();
            }
            catch(IOException ex) {
                log.warning("Failed to stop Firmata device. Whatever.");
            }
        }
    }

    @Override
    public void onDisable() {
        try {
            device.stop();
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
        try {
            inputPin.setMode(Mode.INPUT);
            inputPin.setMode(Mode.PULLUP);
        }
        catch(IOException ex) {
            log.warning("erm???");
        }
    }

}
