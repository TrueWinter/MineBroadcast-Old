package dev.truewinter.minebroadcast;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author TrueWinter
 */
public class MiningMonitor {
    // Every time a player mines a monitored block with counting enabled,
    // add it here. If the mining counter exceeds the configured alertAfter,
    // notify mods. Reset the mining counters every 5 minutes.

    // Example:
    // Player A and B are both mining. Player A mines normally and stays
    // well below the alertAfter value. Player B is x-raying and quickly goes
    // over the alertAfter value. The mods should be alerted of player B's mining
    // counters, including the time since last counter reset.

    // TODO: Test
    // Do 3 test mining sessions:
    // - Branch mining with eff3 diamond pickaxe
    // - Beacon mining with eff3 diamond pickaxe and haste 2
    // - Simulated x-ray (replace non-ore with glass) and high efficiency pickaxe
    //
    // Set values from this in plugin config, then test again.

    private MineBroadcast plugin;
    private Map<String, MonitoredBlock> monitoredBlocks;
    private Map<UUID, PlayerMiningStatistics> playerMiningStatistics = new HashMap<>();
    private LocalDateTime startMin;
    private LocalDateTime endMin;
    private LocalDateTime lastReset;
    private Duration durationMin;
    private int secondsMin;
    private Timer timer = new Timer();

    public MiningMonitor(MineBroadcast plugin, Map<String, MonitoredBlock> monitoredBlocks) {
        this.plugin = plugin;
        this.monitoredBlocks = monitoredBlocks;

        startMin = LocalDateTime.now();
        lastReset = LocalDateTime.now();
        endMin = startMin.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
        durationMin = Duration.between(startMin, endMin);

        // Run 5 seconds before the minute. This is to account for possible server restarts
        secondsMin = (int) (durationMin.toMillis() / 1000) - 5;

        if (secondsMin < 0) {
            //plugin.getLogger().info("M: Adding 60. Old: " + secondsMin);
            secondsMin += 60;
        }

        /*plugin.getLogger().info("M: " + String.valueOf(startMin));
        plugin.getLogger().info("M: " + String.valueOf(startMin.plusSeconds((long) secondsMin)));
        plugin.getLogger().info("M: " + String.valueOf(endMin));
        plugin.getLogger().info("M: " + String.valueOf(secondsMin));*/

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (UUID key : playerMiningStatistics.keySet()) {
                            playerMiningStatistics.get(key).resetTurnMonitorIfNoMiningActivity();
                            playerMiningStatistics.get(key).reset();
                        }
                        lastReset = LocalDateTime.now();
                    }
                }.runTask(plugin);
            }
        }, secondsMin * 1000L, 5 * 60 * 1000L);
    }

    public void addBlockCount(Player player, Block block, int count) {
        // This shouldn't happen, but in case it does, send a warning and ignore this block
        if (!monitoredBlocks.containsKey(block.getType().name())) {
            plugin.getLogger().warning(block.getType().name() + " is not a monitored block");
            return;
        }

        if (!monitoredBlocks.get(block.getType().name()).countingIsEnabled()) {
            return;
        }

        // If the player mines more than alertAfter blocks from one vein, they're probably
        // just using fortune at their base to mine silk-touched blocks
        if (monitoredBlocks.get(block.getType().name()).getAlertAfter() <= count) {
            return;
        }

        initPlayerMiningStatistics(player);

        playerMiningStatistics.get(player.getUniqueId()).addBlockCount(block.getType().name(), count);

        if (shouldAlert(player, block)) {
            alertOnlineMods(player, block);
        }
    }

    public void initPlayerMiningStatistics(Player player) {
        if (!playerMiningStatistics.containsKey(player.getUniqueId())) {
            playerMiningStatistics.put(player.getUniqueId(), new PlayerMiningStatistics(plugin, player));
        }
    }

    private boolean shouldAlert(Player player, Block block) {
        PlayerMiningStatistics s = playerMiningStatistics.get(player.getUniqueId());
        MonitoredBlock m = monitoredBlocks.get(block.getType().name());

        if (!m.countingIsEnabled()) return false;
        if (!player.hasPermission("mb.broadcast")) return false;
        // After an alert, do not alert again until the player has mined the alertAfter block count *again*
        // Example: If the alertAfter is 10, and the player has triggered an alert, only alert again at 20, and then 30, etc.
        if (s.getLastAlert(block.getType().name()) + m.getAlertAfter() > s.getBlockCount(block.getType().name())) return false;

        return s.getBlockCount(block.getType().name()) > m.getAlertAfter();
    }

    private void alertOnlineMods(Player player, Block block) {
        PlayerMiningStatistics s = playerMiningStatistics.get(player.getUniqueId());
        int count = s.getBlockCount(block.getType().name());
        MonitoredBlock m = monitoredBlocks.get(block.getType().name());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("mb.receive") && !plugin.isOptOut(p)) {
                p.sendMessage(format(
                        plugin.getConfig().getString("alertMessage"),
                        player,
                        count,
                        block.getType().name(),
                        m.getColor(),
                        count > 1,
                        plugin.formatTime((int) Duration.between(lastReset, LocalDateTime.now()).getSeconds())
                ));
            }
        }

        plugin.getLogger().info(format(
                plugin.getConfig().getString("alertMessage"),
                player,
                count,
                block.getType().name(),
                m.getColor(),
                count > 1,
                plugin.formatTime((int) Duration.between(lastReset, LocalDateTime.now()).getSeconds())
        ));
        s.setLastAlert(block.getType().name(), count);
    }

    public PlayerMiningStatistics getPlayerMiningStatistics(Player player) {
        return playerMiningStatistics.get(player.getUniqueId());
    }

    private String format(String msg, Player player, int count, String block, String color, boolean plural, String time) {
        return ChatColor.translateAlternateColorCodes('&', msg
                .replace("{player_name}", player.getDisplayName())
                .replace("{real_player_name}", player.getName())
                .replace("{world}", player.getWorld().getName())
                .replace("{count}", String.valueOf(count))
                .replace("{block}", translateBlock(block, color))
                .replace("{block_color}", "&" + ChatColor.valueOf(color).getChar())
                .replace("{plural}", plural ? plugin.getConfig().getString("plural", "s") : "")
                .replace("{time}", time)
        );
    }

    private String translateBlock(String block, String color) {
        return "&" + ChatColor.valueOf(color).getChar()
                + plugin.getMonitoredBlock(block).getName();
    }

    public LocalDateTime getLastReset() {
        return lastReset;
    }

    public void cancelTimer() {
        if (timer == null) {
            return;
        }

        timer.cancel();
    }
}
