package com.nqind.wirecraft;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.transport.NetworkTransport;

public class WirecraftPlugin extends JavaPlugin implements Listener {

    IODevice device;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        device = new FirmataDevice("/dev/ttyUSB0");
        try {
            device.start();
            device.ensureInitializationIsDone();
        }
        catch(Exception ex) {
            getLogger().info("failed successfully");
        }
        getLogger().info("enabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

}
