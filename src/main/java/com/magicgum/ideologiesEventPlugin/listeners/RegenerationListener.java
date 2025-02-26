package com.magicgum.ideologiesEventPlugin.listeners;

import com.magicgum.ideologiesEventPlugin.IdeologiesEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.Map;

public class RegenerationListener implements Listener {
    private final IdeologiesEventPlugin plugin;
    private final Map<String, Double> regenModifiers;
    private final double maxHealth;

    public RegenerationListener(IdeologiesEventPlugin plugin) {
        this.plugin = plugin;
        this.regenModifiers = plugin.getRegenModifiers();
        this.maxHealth = plugin.getMaxHealth();
    }

    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Adjust the regained amount by multiplier
            double originalRegen = event.getAmount();
            double modifier = regenModifiers.getOrDefault(event.getRegainReason().name(), 1.0);
            event.setAmount(originalRegen * modifier);

            // After applying regen, ensure player's health won't exceed maxHealth
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double currentHealth = player.getHealth();
                if (currentHealth > maxHealth) {
                    player.setHealth(maxHealth);
                }
            }, 1L); // 1 tick later
        }
    }
}
