package com.magicgum.ideologiesEventPlugin.listeners;

import com.magicgum.ideologiesEventPlugin.IdeologiesEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class playerJoinListener implements Listener {
    private final IdeologiesEventPlugin plugin;
    private final boolean spectatorHeadsEnabled;

    public playerJoinListener(IdeologiesEventPlugin plugin) {
        this.plugin = plugin;
        this.spectatorHeadsEnabled = plugin.getSpectatorHeadsEnabled();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player target = event.getPlayer();

        if (!spectatorHeadsEnabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    player.hidePlayer(plugin, target);
                    target.hidePlayer(plugin, player);
                }
            }
        }
    }
}
