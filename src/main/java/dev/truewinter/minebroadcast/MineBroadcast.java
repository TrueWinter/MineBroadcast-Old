package dev.truewinter.minebroadcast;

// This plugin is a modified version of bendem's MineBroadcast plugin

import dev.truewinter.minebroadcast.commands.Command;
import dev.truewinter.minebroadcast.commands.CommandHandler;
import dev.truewinter.minebroadcast.handlers.BlockBreakListener;
import dev.truewinter.minebroadcast.handlers.BlockPlaceListener;
import dev.truewinter.minebroadcast.handlers.PistonListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * MineBroadcast for Bukkit
 *
 * @author bendem
 * @author TrueWinter
 */
public class MineBroadcast extends JavaPlugin {

    private Config config;

    @Override
    public void onEnable() {
        config = new Config(this);
        config.loadConfig();

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
     * Checks wether a material should be broadcasted when broken
     *
     * @param material the material to check
     * @return true if breaking a block of that material will trigger a
     *         broadcast
     */
    public boolean isWhitelisted(Material material) {
        return config.getBlocksToBroadcast().contains(material);
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

}
