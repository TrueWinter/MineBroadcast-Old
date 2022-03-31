package dev.truewinter.minebroadcast;

import dev.truewinter.minebroadcast.events.MiningTurnMonitorEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TrueWinter
 */
public class PlayerMiningStatistics {
    private Map<String, Integer> minedBlocks = new HashMap<>();
    private Map<String, Integer> lastAlerts = new HashMap<>();
    private MiningTurnMonitor miningTurnMonitor;
    private StatisticsSaver statisticsSaver;
    private Player player;
    private MineBroadcast plugin;
    private boolean shouldMonitorSuspiciousMining = false;
    private int suspiciousMining = 0;

    private final int SUSPICIOUS_MINING_ALERT = 3;

    public PlayerMiningStatistics(MineBroadcast plugin, Player player) {
        this.player = player;
        this.plugin = plugin;

        miningTurnMonitor = new MiningTurnMonitor(plugin, this.player);

        miningTurnMonitor.setMiningTurnMonitorEvent(new MiningTurnMonitorEvent() {
            @Override
            public void didTurnEvent() {
                //System.out.println("Direction change");
                shouldMonitorSuspiciousMining = true;
            }

            @Override
            public void didMineForLongAfterTurn() {
                //System.out.println("Mined for long");
                shouldMonitorSuspiciousMining = false;
            }

            @Override
            public void resetTurnMonitor() {
                suspiciousMining = 0;
                shouldMonitorSuspiciousMining = false;
            }

            @Override
            public void resetTurnMonitorAfterNoActivity() {
                // TODO: handle no activity in a better way
                suspiciousMining = 0;
                shouldMonitorSuspiciousMining = false;
            }
        });
    }

    // adds monitored block vein
    public void addBlockCount(String block, int count) {
        if (!minedBlocks.containsKey(block)) {
            minedBlocks.put(block, 0);
        }

        int b = minedBlocks.get(block);
        b += count;
        minedBlocks.put(block, b);

        if (shouldMonitorSuspiciousMining) {
            suspiciousMining++;
            shouldMonitorSuspiciousMining = false;

            //System.out.println("Suspicious mining: " + suspiciousMining);
        }

        if (suspiciousMining == SUSPICIOUS_MINING_ALERT) {
            suspiciousMining = 0;
            shouldMonitorSuspiciousMining = false;

            suspiciousMiningAlert();
        }
    }

    private void suspiciousMiningAlert() {
        String suspiciousMiningAlert = plugin.getConfig().getString("suspiciousMiningAlertMessage")
                .replace("{player_name}", player.getDisplayName())
                .replace("{real_player_name}", player.getName())
                .replace("{world}", player.getWorld().getName());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("mb.receive") && !plugin.isOptOut(p)) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', suspiciousMiningAlert));
            }
        }

        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', suspiciousMiningAlert));
    }

    public int getBlockCount(String block) {
        return minedBlocks.get(block);
    }

    public Map<String, Integer> getBlockCounts() {
        return minedBlocks;
    }

    public Player getPlayer() {
        return player;
    }

    public int getLastAlert(String block) {
        if (lastAlerts.get(block) == null) {
            return 0;
        }

        return lastAlerts.get(block);
    }

    public Map<String, Integer> getLastAlerts() {
        return lastAlerts;
    }

    public void setLastAlert(String block, int count) {
        this.lastAlerts.put(block, count);
    }

    public MiningTurnMonitor getMiningTurnMonitor() {
        return miningTurnMonitor;
    }

    public StatisticsSaver getStatisticsSaver() {
        if (statisticsSaver == null) {
            statisticsSaver = new StatisticsSaver(plugin, player);
        }

        return statisticsSaver;
    }

    // Only reset the MiningTurnMonitor if the player has not
    // mined any monitored blocks since last reset() call
    public void resetTurnMonitorIfNoMiningActivity() {
        /*if (minedBlocks.size() == 0) {
            miningTurnMonitor.resetTurnMonitor();
        }*/

        miningTurnMonitor.resetTurnMonitorIfNoActivity();
    }

    public void reset() {
        minedBlocks.clear();
        lastAlerts.clear();

        //miningTurnMonitor.resetTurnMonitor();
    }
}
