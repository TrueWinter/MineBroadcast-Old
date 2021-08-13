package dev.truewinter.minebroadcast;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * @author bendem
 */
public class PlayerLoginListener implements Listener {

    private final MineBroadcast plugin;

    public PlayerLoginListener(MineBroadcast plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {

    }

}
