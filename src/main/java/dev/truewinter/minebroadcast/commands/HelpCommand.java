package dev.truewinter.minebroadcast.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * @author bendem
 * @author TrueWinter
 */
public class HelpCommand extends Command {

    private final CommandHandler handler;
    private JavaPlugin plugin;

    protected HelpCommand(CommandHandler handler, JavaPlugin plugin) {
        super("help", "Displays the commands you can use", null);
        this.handler = handler;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        StringBuilder builder = new StringBuilder("MineBroadcast v" + plugin.getDescription().getVersion() + "\n");
        builder.append(ChatColor.LIGHT_PURPLE + "MineBroadcast by TrueWinter is a fork of OreBroadcast by bendem" + ChatColor.RESET + "\n");
        for(Command command : handler.getCommands().values()) {
            if(command.hasPermission(sender)) {
                builder.append("- ").append(ChatColor.BLUE).append(command.getName()).append(ChatColor.RESET);
                if(command.getDescription() != null) {
                    builder.append(": ").append(command.getDescription());
                }
                builder.append('\n');
            }
        }
        sender.sendMessage(builder.deleteCharAt(builder.length() - 1).toString());
    }

}
