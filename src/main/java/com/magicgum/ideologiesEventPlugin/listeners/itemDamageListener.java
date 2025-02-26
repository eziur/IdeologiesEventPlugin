package com.magicgum.ideologiesEventPlugin.listeners;

import com.magicgum.ideologiesEventPlugin.IdeologiesEventPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class itemDamageListener implements Listener {
    private final double durabilityModifier;

    public itemDamageListener(IdeologiesEventPlugin plugin) {
        this.durabilityModifier = plugin.getDurabilityModifier();
    }

    // Control item durability, durability damage is multiplied by globalDurability Modifier
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        // event.getDamage() is how much durability is lost
        int originalDamage = event.getDamage();

        double adjustedDamage = originalDamage * durabilityModifier;
        int finalDamage = (int) Math.round(adjustedDamage);

        event.setDamage(finalDamage);
    }
}
