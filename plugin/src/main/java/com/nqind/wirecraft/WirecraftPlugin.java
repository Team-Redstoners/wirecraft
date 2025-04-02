package com.nqind.wirecraft;

import net.kyori.adventure.text.Component;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.firmata4j.Pin.Mode;
import org.firmata4j.transport.NetworkTransport;
import org.firmata4j.transport.SerialTransport;
import org.firmata4j.transport.TransportInterface;

import com.nqind.wirecraft.device.S3DevKit;
import com.nqind.wirecraft.device.WirecraftDevice;
import com.nqind.wirecraft.device.WirecraftDevice.CoordDirection;

public class WirecraftPlugin extends JavaPlugin implements Listener {

    WirecraftDevice device;
    FileConfiguration config = getConfig();
    BukkitTask inputTask;
    ConcurrentLinkedQueue<WireEvent> eventQ = new ConcurrentLinkedQueue<WireEvent>();

    Material matOn;
    Material matOff;

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
        config.addDefault("materialOn", "REDSTONE_BLOCK");
        config.addDefault("materialOff", "STONE");
        config.options().copyDefaults(true);
        saveConfig();
        matOn = Material.valueOf(config.getString("materialOn"));
        matOff = Material.valueOf(config.getString("materialOff"));

        // Get Bukkit world
        // Even freaking md_5 themselves says to use getWorlds().get(0) for the main world
        // Wtf md_5
        mainWorld = getServer().getWorlds().getFirst();

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
            device.setLocation(new int[]{0,64,0}, CoordDirection.SOUTH);

            log.info("Wirecraft Plugin enabled!");

            // Start input tick task
            inputTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    if(true) {
                        if(!eventQ.isEmpty()) {
                            WireEvent event = eventQ.remove();
                            log.log(Level.INFO, "Processing event: wire pin #" + event.pinNum + " went " + event.state);
                            // Convert wirecraft pin to RS pin
                            int rsPin = device.firmataPinToRSPin(event.pinNum);
                            // Convert RS pin to relative coordinate offset
                            // X is either 0 or 3 - odd is 0, even is 3
                            int offX = (1 - (rsPin % 2)) * 3;
                            int oldOffX = offX;  // saved for east/west where coords are swapped
                            // Z is just (pin-1)/2 floored, multiplied by 2 again
                            // Z=0 has pins 1,2, Z=2 has pins 3,4, etc
                            int offZ = Math.floorDiv(rsPin - 1, 2) * 2;
                            // Now rotate the coordinates in multiples of 90 degrees based on the device orientation
                            switch(device.getDirection()) {
                                case NORTH:
                                    // Mirror both coordinates
                                    offX = -offX;
                                    offZ = -offZ;
                                    break;
                                case EAST:
                                    offX = offZ;
                                    offZ = -oldOffX;
                                    break;
                                case WEST:
                                    offX = -offZ;
                                    offZ = oldOffX;
                                    break;
                                case SOUTH:
                                    // Do nothing
                                    break;
                                default:
                                  log.log(Level.WARNING, "Unknown direction", device.getDirection());
                                    break;
                            }
                            int[] pos = device.getLocation();
                            pos[0] += offX;
                            pos[2] += offZ;
                            Material mat = event.state ? Material.REDSTONE_BLOCK : Material.STONE;
                            Block block = mainWorld.getBlockAt(pos[0], pos[1], pos[2]);
                            block.setType(mat);
                            log.info("Set block {" + pos[0] + "," + pos[1] + "," + pos[2] + "} to " + mat.toString());
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
        catch(Exception e) {
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
        int rsPin = device.coordToRSPin(block.getX(), block.getY(), block.getZ());
        if(rsPin < 0) {
            return;
        }
        // Crappy way of checking if pin is an output
        if(block.getType() == Material.REDSTONE_LAMP) {
            boolean powered = block.isBlockPowered();
            device.setOutputPin(rsPin, powered);
            getServer().broadcast(Component.text("set " + rsPin + " to " + powered));
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if(event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player p = event.getPlayer();
            Block block = event.getClickedBlock();
            int rsPin = device.coordToRSPin(block.getX(), block.getY(), block.getZ());
            if(rsPin < 0) {
                return;
            }
            Mode newMode = block.getType() == Material.REDSTONE_LAMP ? Mode.INPUT : Mode.OUTPUT;
            if(device.setMode(rsPin, newMode)) {
                p.sendMessage("Set pin " + rsPin + " to " + newMode.name());
                block.setType(newMode == Mode.INPUT ? matOff : Material.REDSTONE_LAMP);
            }
            else {
                p.sendMessage("Failed to change pin direction. Is this a valid pin?");
            }

        }
    }
}
