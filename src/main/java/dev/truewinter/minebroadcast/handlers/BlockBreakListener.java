package dev.truewinter.minebroadcast.handlers;

import dev.truewinter.minebroadcast.MineBroadcast;
import dev.truewinter.minebroadcast.MineBroadcastEvent;
import dev.truewinter.minebroadcast.MineBroadcastException;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * @author bendem
 * @author TrueWinter
 */
public class BlockBreakListener implements Listener {

    private final MineBroadcast plugin;

    public BlockBreakListener(MineBroadcast plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        // Reject
        // + creative users
        // + users without mb.broadcast permission
        // + users in a non whitelisted world
        if(player.getGameMode() != GameMode.SURVIVAL
                || !player.hasPermission("mb.broadcast")
                || !plugin.isWorldWhitelisted(player.getWorld().getName())) {
            return;
        }

        Block block = event.getBlock();

        plugin.getMiningMonitor().initPlayerMiningStatistics(player);
        plugin.getMiningMonitor().getPlayerMiningStatistics(player).getMiningTurnMonitor()
                .setLastBlockPos(block.getX(), block.getY(), block.getZ());
        plugin.getMiningMonitor().getPlayerMiningStatistics(player).getStatisticsSaver()
                .addMinedBlock(block);

        // Don't broadcast blacklisted blocks
        if(plugin.isBlackListed(block)) {
            plugin.unBlackList(block);
            return;
        }

        if(!plugin.isWhitelisted(block.getType())) {
            return;
        }

        // Measuring event time
        long timer = System.currentTimeMillis();

        Set<Block> vein = getVein(block);
        if(vein == null || vein.size() < 1) {
            plugin.getLogger().fine("Vein ignored");
            return;
        }

        // Get recipients
        Set<Player> recipients = new HashSet<>();
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if(onlinePlayer.hasPermission("mb.receive") && !plugin.isOptOut(onlinePlayer)) {
                recipients.add(onlinePlayer);
            }
        }

        MineBroadcastEvent e = new MineBroadcastEvent(
            plugin.getConfig().getString("message", "{player} just found {count} block{plural} of {block}"),
            player,
            block,
            recipients,
            vein
        );

        plugin.getServer().getPluginManager().callEvent(e);
        if(e.isCancelled() || e.getVein().isEmpty()) {
            return;
        }

        plugin.blackList(e.getVein());
        plugin.unBlackList(e.getBlockMined());

        String blockName;
        blockName = e.getBlockMined().getType().name().toLowerCase();

        //String color = plugin.getConfig().getString("colors." + blockName, "white").toUpperCase();
        String color = plugin.getMonitoredBlock(blockName.toUpperCase()).getColor().toUpperCase();
        String formattedMessage = format(
            e.getFormat(),
            e.getSource(),
            e.getVein().size(),
            blockName,
            color,
            e.getVein().size() > 1
        );
        broadcast(e.getRecipients(), formattedMessage);
        plugin.getLogger().info(formattedMessage);
        plugin.getMiningMonitor().addBlockCount(player, block, e.getVein().size());

        if(plugin.getConfig().getBoolean("timing-debug", false)) {
            plugin.getLogger().info("Event duration : " + (System.currentTimeMillis() - timer) + "ms");
        }
    }

    private Set<Block> getVein(Block block) {
        Set<Block> vein = new HashSet<>();
        vein.add(block);
        try {
            getVein(block, vein);
        } catch(MineBroadcastException e) {
            return null;
        }
        return vein;
    }

    private void getVein(Block block, Set<Block> vein) throws MineBroadcastException {
        if(vein.size() > plugin.getConfig().getInt("max-vein-size", 100)) {
            throw new MineBroadcastException();
        }

        int i, j, k;
        for (i = -1; i < 2; ++i) {
            for(j = -1; j < 2; ++j) {
                for(k = -1; k < 2; ++k) {
                    Block relative = block.getRelative(i, j, k);
                    if(!vein.contains(relative)                  // block already found
                            && equals(block, relative)           // block has not the same type
                            && ((i != 0 || j != 0 || k != 0))    // comparing block to itself
                            && !plugin.isBlackListed(relative)) {// don't consider blacklisted blocks
                        vein.add(relative);
                        getVein(relative, vein);
                    }
                }
            }
        }
    }

    // Workaround for redstone ores, probably no longer needed
    private boolean equals(Block block1, Block block2) {
        return block1.getType().equals(block2.getType());
    }

    private void broadcast(Set<Player> recipients, String message) {
        for (Player recipient : recipients) {
            recipient.sendMessage(message);
        }
    }

    private String format(String msg, Player player, int count, String block, String color, boolean plural) {
        return ChatColor.translateAlternateColorCodes('&', msg
            .replace("{player_name}", player.getDisplayName())
            .replace("{real_player_name}", player.getName())
            .replace("{world}", player.getWorld().getName())
            .replace("{count}", String.valueOf(count))
            .replace("{block}", translateBlock(block, color))
            .replace("{block_color}", "&" + ChatColor.valueOf(color).getChar())
            .replace("{plural}", plural ? plugin.getConfig().getString("plural", "s") : "")
        );
    }

    private String translateBlock(String block, String color) {
        return "&" + ChatColor.valueOf(color).getChar()
            + plugin.getMonitoredBlock(block).getName();
    }

}
