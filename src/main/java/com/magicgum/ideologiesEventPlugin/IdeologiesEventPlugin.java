package com.magicgum.ideologiesEventPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;

public class IdeologiesEventPlugin extends JavaPlugin implements Listener {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject configJson;

    // Cached config values
    private double healthRegenRate;
    private double maxHealth;
    private double globalDurabilityModifier;

    @Override
    public void onEnable() {
        // Load (or create) our JSON config
        loadConfigJson();

        // Load config values into fields
        applyConfigValues();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("MyPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MyPlugin has been disabled!");
    }

    private void loadConfigJson() {
        try {
            File configFile = new File(getDataFolder(), "config.json");

            // If there's no plugin data folder yet, create it
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // If config.json doesn't exist, copy default from resources
            if (!configFile.exists()) {
                InputStream defaultConfigStream = getResource("default_config.json");
                if (defaultConfigStream != null) {
                    Files.copy(defaultConfigStream, configFile.toPath());
                    getLogger().info("Default config.json created!");
                } else {
                    getLogger().warning("No default_config.json found in resources!");
                }
            }

            // Now read the config file
            try (FileReader reader = new FileReader(configFile)) {
                configJson = JsonParser.parseReader(reader).getAsJsonObject();
            }

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Error loading config.json: " + e.getMessage());
            // Create a fallback empty config if something goes wrong
            configJson = new JsonObject();
        }
    }

    private void applyConfigValues() {
        // Set defaults if they are missing in the JSON
        healthRegenRate = configJson.has("health_regen_rate")
                ? configJson.get("health_regen_rate").getAsDouble()
                : 1.0;

        maxHealth = configJson.has("max_health")
                ? configJson.get("max_health").getAsDouble()
                : 20.0;

        globalDurabilityModifier = configJson.has("global_durability_modifier")
                ? configJson.get("global_durability_modifier").getAsDouble()
                : 1.0;

        // Apply the max health to all online players immediately
        for (Player player : Bukkit.getOnlinePlayers()) {
            setPlayerMaxHealth(player, maxHealth);
        }
    }

    private void saveConfigJson() {
        try {
            File configFile = new File(getDataFolder(), "config.json");

            // Update our in-memory JsonObject
            configJson.addProperty("health_regen_rate", healthRegenRate);
            configJson.addProperty("max_health", maxHealth);
            configJson.addProperty("global_durability_modifier", globalDurabilityModifier);

            // Write to disk
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(configJson, writer);
            }

            getLogger().info("Config saved to config.json!");
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Error saving config.json: " + e.getMessage());
        }
    }

    // Event Listeners

    // Control health regeneration rate and cap max health.
    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            // Adjust the regained amount by our multiplier
            double originalRegen = event.getAmount();
            double modifiedRegen = originalRegen * healthRegenRate;
            event.setAmount(modifiedRegen);

            // After applying regen, we also want to ensure player's health won't exceed maxHealth
            // We'll do that by scheduling a small task afterward or by quick check.
            Bukkit.getScheduler().runTaskLater(this, () -> {
                double currentHealth = player.getHealth();
                if (currentHealth > maxHealth) {
                    player.setHealth(maxHealth);
                }
            }, 1L); // 1 tick later
        }
    }

    // Control item durability
    // A globalDurabilityModifier < 1 means items break faster, > 1 means they last longer.
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        // event.getDamage() is how much durability is lost
        int originalDamage = event.getDamage();

        // If globalDurabilityModifier = 2.0, the item takes half the damage (2x durability).
        // If globalDurabilityModifier = 0.5, the item takes double damage (0.5x durability).
        double adjustedDamage = originalDamage / globalDurabilityModifier;

        // Round up or down as you prefer
        int finalDamage = (int) Math.round(adjustedDamage);

        event.setDamage(finalDamage);
    }

    // In Game Command Handling
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage("Console usage: /" + label + " <value>");
        }

        switch (command.getName().toLowerCase()) {
            case "setregenrate":
                return handleSetRegenRate(sender, args);

            case "setmaxhealth":
                return handleSetMaxHealth(sender, args);

            case "setdurabilitymodifier":
                return handleSetDurabilityModifier(sender, args);

            default:
                return false;
        }
    }

    private boolean handleSetRegenRate(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /setRegenRate <value>");
            return true;
        }
        try {
            double value = Double.parseDouble(args[0]);
            healthRegenRate = value;
            saveConfigJson();
            sender.sendMessage("Health regeneration rate set to " + value);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid number!");
        }
        return true;
    }

    private boolean handleSetMaxHealth(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /setMaxHealth <value>");
            return true;
        }
        try {
            double value = Double.parseDouble(args[0]);
            maxHealth = value;

            // Apply immediately to all online players
            for (Player online : Bukkit.getOnlinePlayers()) {
                setPlayerMaxHealth(online, maxHealth);
            }

            saveConfigJson();
            sender.sendMessage("Max health set to " + value + "!");
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid number!");
        }
        return true;
    }

    private boolean handleSetDurabilityModifier(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /setDurabilityModifier <value>");
            return true;
        }
        try {
            double value = Double.parseDouble(args[0]);
            globalDurabilityModifier = value;
            saveConfigJson();
            sender.sendMessage("Global durability modifier set to " + value + "!");
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid number!");
        }
        return true;
    }

    // Helper method to set a player's max health
    private void setPlayerMaxHealth(Player player, double max) {
        LivingEntity entity = player;
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);
        // If player has more HP than new max, clamp it
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
    }
}