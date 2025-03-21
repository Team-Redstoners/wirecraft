package com.nqind.wirecraft;

import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.firmata4j.Pin.Mode;
import org.firmata4j.transport.NetworkTransport;
import org.firmata4j.transport.SerialTransport;
import org.firmata4j.transport.TransportInterface;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.nqind.wirecraft.device.S3DevKit;
import com.nqind.wirecraft.device.WirecraftDevice;

public class WirecraftPlugin extends JavaPlugin implements Listener {

    WirecraftDevice device;
    FileConfiguration config = getConfig();
    BukkitTask inputTask;
    ConcurrentLinkedQueue<WireEvent> eventQ = new ConcurrentLinkedQueue<WireEvent>();

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
        try {
            device = new S3DevKit(0, fmtTransport, eventQ);

            log.info("Wirecraft Plugin enabled!");

            // Start input tick task
            inputTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    if(true) {
                        if(!eventQ.isEmpty()) {
                            WireEvent event = eventQ.remove();
                            // TODO: WHAT DO I DO NEXT!
                            // Convert wirecraft pin to RS pin
                            int rsPin = event.pinNum;
                            int[] pos = (int[])event.get(0);
                            Material mat = (Material)event.get(1);
                            mainWorld.getBlockAt(pos[0], pos[1], pos[2]).setType(mat);
                        }
                    }
                }
            }, 0, 0);
            
        }
        catch(Exception e) {
            device.stop();
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
