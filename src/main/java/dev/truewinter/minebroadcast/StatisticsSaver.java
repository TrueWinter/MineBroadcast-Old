package dev.truewinter.minebroadcast;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * The StatisticsSaver allows for logging *all* mined blocks for a player to a file, reported per minute.
 * It was created for testing and data collecting purposes (such as setting the default ore alerts), and
 * should only be used when necessary and for the time necessary. No effort was made to ensure it works
 * for long-running logging, and is not intended for this purpose.
 * @author TrueWinter
 */
public class StatisticsSaver {
    private Player player;
    private LocalDateTime startTime;
    private Map<Integer, Map<Material, Integer>> minedBlocksPerMinute = new HashMap<>();
    private MineBroadcast plugin;
    private boolean activated = false;

    public StatisticsSaver(MineBroadcast plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void addMinedBlock(Block block) {
        if (!activated) return;

        int minute = (int) Math.floor(Duration.between(startTime, LocalDateTime.now()).getSeconds() / 60F);

        if (minedBlocksPerMinute.get(minute) == null) {
            minedBlocksPerMinute.put(minute, new HashMap<>());
        }

        Map<Material, Integer> m = minedBlocksPerMinute.get(minute);

        if (m.get(block.getType()) == null) {
            m.put(block.getType(), 0);
        }

        m.put(block.getType(), m.get(block.getType()) + 1);

        //System.out.println(minute);
        //System.out.println(block.getType());
        //System.out.println(m.get(block.getType()));
        //System.out.println("-----");
    }

    public void setActivated(boolean activated) {
        if (activated && !isActivated()) {
            startTime = LocalDateTime.now();
        }

        this.activated = activated;
    }

    public boolean isActivated() {
        return activated;
    }

    public void save(File file) {
        String output = "";

        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        output += player.getName() + " (" + player.getUniqueId() + "). Start: " + time.format(formatter) + "\n\n";

        for (Integer min : minedBlocksPerMinute.keySet()) {
            output += "Minute " + min + "\n";

            for (Material m : minedBlocksPerMinute.get(min).keySet()) {
                output += m + ": " + minedBlocksPerMinute.get(min).get(m) + "\n";
            }

            output += "\n";
        }

        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(output);
            fileWriter.close();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write statistics data to file");
            e.printStackTrace(System.err);
        }

        this.reset();
    }

    public void reset() {
        this.activated = false;
        minedBlocksPerMinute.clear();
    }

    public Player getPlayer() {
        return player;
    }

    public boolean hasData() {
        return !minedBlocksPerMinute.isEmpty();
    }
}
