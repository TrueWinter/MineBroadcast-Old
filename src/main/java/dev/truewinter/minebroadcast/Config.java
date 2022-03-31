package dev.truewinter.minebroadcast;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import javax.management.monitor.Monitor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * @author bendem
 * @author TrueWinter
 */
public class Config {
    private final MineBroadcast plugin;
    private final Set<SafeBlock> broadcastBlacklist = new HashSet<>();
    //private final Set<Material> blocksToBroadcast = new HashSet<>();
    private final Map<String, MonitoredBlock> monitoredBlocks = new HashMap<>();
    private final Set<String> worldWhitelist = new HashSet<>();
    private final Set<UUID> optOutPlayers = new HashSet<>();
    private final File playerFile;
    private boolean worldWhitelistActive = false;

    /* package */ Config(MineBroadcast plugin) {
        this.plugin = plugin;
        playerFile = new File(plugin.getDataFolder(), "players.dat");
        plugin.saveDefaultConfig();
    }

    /* package */ void loadConfig() {
        plugin.reloadConfig();
        // Create the list of materials to broadcast from the file
        List<String> configList = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("blocks");

        monitoredBlocks.clear();

        for (String key : sec.getKeys(false)) {
            Material material = Material.getMaterial(key.toUpperCase());

            if (material == null) {
                plugin.getLogger().warning("Skipping material " + key + " as it is invalid.");
                continue;
            }

            // To avoid confusion, the warning message should show the value exactly as the user entered it.
            // For this reason, .toUpperCase() is used later in the code.
            String color = plugin.getConfig().getString("blocks." + key + ".color");
            try {
                ChatColor c = ChatColor.valueOf(color.toUpperCase());
            } catch (Exception e) {
                plugin.getLogger().warning("Color " + color + " is invalid, resetting to white.");
                color = "WHITE";
            }

            String name = plugin.getConfig().getString("blocks." + key + ".name");

            if (name.isEmpty()) {
                plugin.getLogger().warning("Name field for " + key + " is required. Skipping.");
                continue;
            }

            boolean counting = plugin.getConfig().getBoolean("blocks." + key + ".counting");
            int alertAfter = plugin.getConfig().getInt("blocks." + key + ".alertAfter");

            if (alertAfter < 1) {
                counting = false;
                plugin.getLogger().warning("Disabled counting for " + key + " as alertAfter value was less than 1");
            }

            MonitoredBlock m = new MonitoredBlock(material, color.toUpperCase(), name, counting, alertAfter);
            monitoredBlocks.put(key.toUpperCase(), m);
        }

        /*blocksToBroadcast.clear();

        for(String item : configList) {
            Material material = Material.getMaterial(item.toUpperCase());
            blocksToBroadcast.add(material);
        }*/

        // Load world whitelist
        worldWhitelist.clear();
        worldWhitelistActive = plugin.getConfig().getBoolean("active-per-worlds", true);
        if(worldWhitelistActive) {
            worldWhitelist.addAll(plugin.getConfig().getStringList("active-worlds"));
        }

        // Load opt out players
        if(!playerFile.exists()) {
            return;
        }
        optOutPlayers.clear();
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(playerFile))) {
            @SuppressWarnings("unchecked")
            Set<UUID> uuids = (Set<UUID>) stream.readObject();
            optOutPlayers.addAll(uuids);
        } catch(IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to read opt out players from file");
            e.printStackTrace(System.err);
        } catch(ClassCastException e) {
            plugin.getLogger().severe("Invalid players.dat file");
            e.printStackTrace(System.err);
        }
    }

    /* package */ boolean isOptOut(UUID uuid) {
        return optOutPlayers.contains(uuid);
    }

    /* package */ void optOutPlayer(UUID uuid) {
        optOutPlayers.add(uuid);
        saveOptOutPlayers();
    }

    /* package */ void unOptOutPlayer(UUID uuid) {
        optOutPlayers.remove(uuid);
        saveOptOutPlayers();
    }

    private void saveOptOutPlayers() {
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(playerFile))) {
            stream.writeObject(optOutPlayers);
        } catch(IOException e) {
            plugin.getLogger().severe("Failed to write opt out players to file");
            e.printStackTrace(System.err);
        }
    }

    /* package */ Set<SafeBlock> getBroadcastBlacklist() {
        return broadcastBlacklist;
    }

    /* package */ /*Set<Material> getBlocksToBroadcast() {
        return blocksToBroadcast;
    }*/

    public Map<String, MonitoredBlock> getMonitoredBlocks() {
        return monitoredBlocks;
    }

    public MonitoredBlock getMonitoredBlock(String block) {
        if (!monitoredBlocks.containsKey(block.toUpperCase())) {
            return null;
        }

        return monitoredBlocks.get(block.toUpperCase());
    }

    /* package */ Set<String> getWorldWhitelist() {
        return worldWhitelist;
    }

    /* package */ boolean isWorldWhitelistActive() {
        return worldWhitelistActive;
    }

}
