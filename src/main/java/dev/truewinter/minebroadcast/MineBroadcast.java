package dev.truewinter.minebroadcast;

// This plugin is a modified version of bendem's OreBroadcast plugin

import dev.truewinter.minebroadcast.commands.Command;
import dev.truewinter.minebroadcast.commands.CommandHandler;
import dev.truewinter.minebroadcast.handlers.BlockBreakListener;
import dev.truewinter.minebroadcast.handlers.BlockPlaceListener;
import dev.truewinter.minebroadcast.handlers.PistonListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MineBroadcast for Bukkit
 *
 * @author bendem
 * @author TrueWinter
 */
public class MineBroadcast extends JavaPlugin {

    private Config config;
    private MiningMonitor miningMonitor;

    @Override
    public void onEnable() {
        config = new Config(this);
        config.loadConfig();
        miningMonitor = new MiningMonitor(this, config.getMonitoredBlocks());

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PistonListener(this), this);

        CommandHandler commandHandler = new CommandHandler(this, "mb");
        commandHandler.register(new Command("clear", "Clears blacklisted blocks", "mb.commands.clear") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                int size = clearBlackList();
                sender.sendMessage(size + " block" + (size > 1 ? "s" : "")  + " cleared...");
            }
        });

        commandHandler.register(new Command("reload", "Reloads MineBroadcast config", "mb.commands.reload") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                config.loadConfig();
                sender.sendMessage("Config reloaded...");
            }
        });

        commandHandler.register(new Command("optout", "Switches off MineBroadcast messages for yourself", "mb.commands.optout") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can optout");
                    return;
                }
                UUID uuid = ((Player) sender).getUniqueId();
                if(config.isOptOut(uuid)) {
                    sender.sendMessage("You have already opted out of MineBroadcast broadcasts");
                } else {
                    config.optOutPlayer(uuid);
                    sender.sendMessage("You won't receive MineBroadcast broadcasts anymore");
                }
            }
        });

        commandHandler.register(new Command("optin", "Switches on MineBroadcast messages for yourself", "mb.commands.optin") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can optin");
                    return;
                }
                UUID uuid = ((Player) sender).getUniqueId();
                if(config.isOptOut(uuid)) {
                    config.unOptOutPlayer(uuid);
                    sender.sendMessage("You will now receive the MineBroadcast broadcasts");
                } else {
                    sender.sendMessage("You have already opted in to MineBroadcast broadcasts");
                }
            }
        });

        commandHandler.register(new Command("statistics", "Get mining statistics for a player", "mb.command.statistics") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                if (args.size() != 1) {
                    sender.sendMessage(ChatColor.RED + "Player name is required.");
                    return;
                }

                Player p = Bukkit.getPlayer(args.get(0));

                if (p == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Unable to find that player. Are they online?");
                    return;
                }

                PlayerMiningStatistics playerMiningStatistics = miningMonitor.getPlayerMiningStatistics(p);
                String time = formatTime((int) Duration.between(miningMonitor.getLastReset(), LocalDateTime.now()).getSeconds());
                String message = p.getDisplayName() + " has found the following in the past " + time +": \n";

                if (playerMiningStatistics == null) {
                    sender.sendMessage(p.getDisplayName() + " has not found any monitored blocks in the past " + time);
                    return;
                }

                Map<String, Integer> miningCounts = playerMiningStatistics.getBlockCounts();

                if (miningCounts.size() == 0) {
                    sender.sendMessage(p.getDisplayName() + " has not found any monitored blocks in the past " + time);
                    return;
                }

                for (String key : miningCounts.keySet()) {
                    message += key + ": " + miningCounts.get(key) + " blocks\n";
                }

                sender.sendMessage(message.trim());
            }
        });

        commandHandler.register(new Command("startstats", "Starts the StatisticsSaver for a player. Use the StatisticsSaver only when needed.", "mb.commands.startstats") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command");
                    return;
                }

                Player player = null;

                if (args.size() == 1) {
                    Player p = Bukkit.getPlayer(args.get(0));
                    if (p != null) {
                        player = p;
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "No player specified (or invalid command syntax). Assuming self-monitor");
                    player = (Player) sender;
                }

                if (player == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Unable to find that player. Are they online?");
                    return;
                }

                getMiningMonitor().initPlayerMiningStatistics(player);
                StatisticsSaver s = getMiningMonitor().getPlayerMiningStatistics(player).getStatisticsSaver();

                if (s.isActivated()) {
                    sender.sendMessage(ChatColor.YELLOW + "The StatisticsSaver is already active for that player");
                    return;
                }

                s.setActivated(true);
                sender.sendMessage(ChatColor.GREEN + "Now logging all mined blocks for that player. " + ChatColor.RED + "Do not run this for longer than needed.");
            }
        });

        commandHandler.register(new Command("stopstats", "Stops the StatisticsSaver for a player", "mb.commands.stopstats") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command");
                    return;
                }

                Player player = null;

                if (args.size() == 1) {
                    Player p = Bukkit.getPlayer(args.get(0));
                    if (p != null) {
                        player = p;
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "No player specified (or invalid command syntax). Assuming self-monitor");
                    player = (Player) sender;
                }

                if (player == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Unable to find that player. Are they online?");
                    return;
                }

                getMiningMonitor().initPlayerMiningStatistics(player);
                StatisticsSaver s = getMiningMonitor().getPlayerMiningStatistics(player).getStatisticsSaver();

                if (!s.isActivated()) {
                    sender.sendMessage(ChatColor.YELLOW + "The StatisticsSaver is not active for that player");
                    return;
                }

                s.setActivated(false);
                sender.sendMessage(ChatColor.GREEN + "Stopped logging mined blocks for that player, saving results to file");

                if (!s.hasData()) {
                    sender.sendMessage(ChatColor.YELLOW + "No data to save");
                    s.reset();
                    return;
                }

                LocalDateTime time = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
                String fileName = s.getPlayer().getUniqueId() + "-" + time.format(formatter) + ".txt";
                File file = new File(getDataFolder(), fileName);

                s.save(file);
            }
        });

        commandHandler.register(new Command("resetplayer", "Resets PlayerMiningStatistics/MiningMonitor for this player", "mb.commands.resetplayer") {
            @Override
            public void execute(CommandSender sender, List<String> args) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command");
                    return;
                }

                Player player = null;

                if (args.size() == 1) {
                    Player p = Bukkit.getPlayer(args.get(0));
                    if (p != null) {
                        player = p;
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "No player specified (or invalid command syntax)");
                    return;
                }

                if (player == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Unable to find that player. Are they online?");
                    return;
                }

                getMiningMonitor().initPlayerMiningStatistics(player);
                getMiningMonitor().getPlayerMiningStatistics(player).getMiningTurnMonitor().resetTurnMonitor();
                getMiningMonitor().getPlayerMiningStatistics(player).reset();
                sender.sendMessage(ChatColor.GREEN + "Reset PlayerMiningStatistics/MiningMonitor for that player");
            }
        });
    }

    public String formatTime(int seconds) {
        int _minutes = (int) Math.floor(seconds / 60);
        int _seconds = seconds - (_minutes * 60);
        String output = "";

        if (_minutes != 0) {
            output += _minutes + (_minutes == 1 ? " minute " : " minutes ");
        }

        if (_seconds != 0) {
            output += _seconds + (_seconds == 1 ? " second" : " seconds");
        }

        return output.trim();
    }

    @Override
    public void onDisable() {
        miningMonitor.cancelTimer();
    }

    /* package */ File getJar() {
        return getFile();
    }

    public boolean isOptOut(Player player) {
        return config.isOptOut(player.getUniqueId());
    }

    /**
     * Blacklists a block. Blocks blacklisted won't get broadcasted when
     * broken.
     *
     * @param block the block to blacklist
     */
    public void blackList(Block block) {
        config.getBroadcastBlacklist().add(new SafeBlock(block));
    }

    /**
     * Blacklists multiple blocks. Blocks blacklisted won't get broadcasted
     * when broken.
     *
     * @param blocks the blocks to blacklist
     */
    public void blackList(Collection<Block> blocks) {
        for(Block block : blocks) {
            blackList(block);
        }
    }

    /**
     * Unblacklist a block.
     *
     * @param block the block to unblacklist
     */
    public void unBlackList(Block block) {
        config.getBroadcastBlacklist().remove(new SafeBlock(block));
    }

    /**
     * Unblacklist multiple blocks.
     *
     * @param blocks the blocks to unblacklist
     */
    public void unBlackList(Collection<Block> blocks) {
        for(Block block : blocks) {
            unBlackList(block);
        }
    }

    /**
     * Clear the blacklist.
     *
     * @return Count of blocks removed from the blacklist
     */
    public int clearBlackList() {
        int size = config.getBroadcastBlacklist().size();
        config.getBroadcastBlacklist().clear();
        return size;
    }

    /**
     * Checks wether a block is blacklisted or not.
     *
     * @param block the block to check
     * @return true if the block is blacklisted
     */
    public boolean isBlackListed(Block block) {
        return config.getBroadcastBlacklist().contains(new SafeBlock(block));
    }

    /**
     * Checks whether a material should be broadcasted when broken
     *
     * @param material the material to check
     * @return true if breaking a block of that material will trigger a
     *         broadcast
     */
    public boolean isWhitelisted(Material material) {
        return config.getMonitoredBlocks().containsKey(material.name());
        //return config.getBlocksToBroadcast().contains(material);
    }

    /**
     * Returns map of monitored blocks
     *
     * @return Returns a map of monitored blocks
     */
    public Map<String, MonitoredBlock> getMonitoredBlocks() {
        return config.getMonitoredBlocks();
    }

    /**
     * Returns a monitored block
     *
     * @param material the material to get
     * @return the monitored block
     */
    public MonitoredBlock getMonitoredBlock(String material) {
        return config.getMonitoredBlock(material);
    }

    /**
     * Check if MineBroadcast is active in a world
     *
     * @param world the world to check if MineBroadcast is active in
     * @return true if MineBroadcast is active in the world
     */
    public boolean isWorldWhitelisted(World world) {
        return isWorldWhitelisted(world.getName());
    }

    /**
     * Check if MineBroadcast is active in a world
     *
     * @param world the name of the world
     * @return true if MineBroadcast is active in the world
     */
    public boolean isWorldWhitelisted(String world) {
        return !config.isWorldWhitelistActive() || config.getWorldWhitelist().contains(world);
    }

    /**
     * Returns the mining monitor
     *
     * @return the mining monitor
     */
    public MiningMonitor getMiningMonitor() {
        return miningMonitor;
    }
}
