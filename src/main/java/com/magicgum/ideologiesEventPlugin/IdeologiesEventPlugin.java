package com.magicgum.ideologiesEventPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magicgum.ideologiesEventPlugin.listeners.RegenerationListener;
import com.magicgum.ideologiesEventPlugin.listeners.itemDamageListener;
import com.magicgum.ideologiesEventPlugin.listeners.playerJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class IdeologiesEventPlugin extends JavaPlugin implements Listener {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject configJson;

    // Cached config values
    private Map<String, Double> regenModifiers = new HashMap<>();
    private double maxHealth;
    private double durabilityModifier;
    private boolean spectatorHeadsEnabled;

    @Override
    public void onEnable() {
        loadConfigJson();
        applyConfigValues();

        // Register listeners
        getServer().getPluginManager().registerEvents(new RegenerationListener(this), this);
        getServer().getPluginManager().registerEvents(new itemDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new playerJoinListener(this), this);

        getLogger().info("IdeologiesPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IdeologiesPlugin disabled!");
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
        maxHealth = configJson.has("max_health") ? configJson.get("max_health").getAsDouble() : 20.0;
        durabilityModifier = configJson.has("global_durability_modifier") ? configJson.get("global_durability_modifier").getAsDouble() : 1.0;
        spectatorHeadsEnabled = configJson.has("spectator_heads_enabled") ? configJson.get("spectator_heads_enabled").getAsBoolean() : true;

        for (EntityRegainHealthEvent.RegainReason reason : EntityRegainHealthEvent.RegainReason.values()) {
            String key = "regen_modifier_" + reason.name().toLowerCase();
            regenModifiers.put(reason.name(), configJson.has(key) ? configJson.get(key).getAsDouble() : 1.0);
        }

        // Apply the max health to all online players immediately
        for (Player player : Bukkit.getOnlinePlayers()) {
            setPlayerMaxHealth(player, maxHealth);
        }
    }

    private void saveConfigJson() {
        try {
            File configFile = new File(getDataFolder(), "config.json");

            // Update our in-memory JsonObject
            configJson.addProperty("max_health", maxHealth);
            configJson.addProperty("global_durability_modifier", durabilityModifier);
            configJson.addProperty("spectator_heads_enabled", spectatorHeadsEnabled);
            regenModifiers.forEach((key, value) -> configJson.addProperty("regen_modifier_" + key.toLowerCase(), value));

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

    // In Game Command Handling
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && (!(sender instanceof Player) || !((Player) sender).isOp())) {
            sender.sendMessage("You must be a server operator to use this command.");
            return true;
        }

        return switch (command.getName().toLowerCase()) {
            case "setregenrate" -> handleSetRegenRate(sender, args);
            case "setmaxhealth" -> handleSetMaxHealth(sender, args);
            case "setdurabilitymodifier" -> handleSetDurabilityModifier(sender, args);
            case "spectatorheads" -> handleSpectatorHeads(sender, args);
            default -> false;
        };
    }

    private boolean handleSetRegenRate(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /setRegenRate <regen_reason> <value>");
            return true;
        }
        try {
            String reason = args[0].toUpperCase();
            double value = Double.parseDouble(args[1]);
            if (regenModifiers.containsKey(reason)) {
                regenModifiers.put(reason, value);
                saveConfigJson();
                sender.sendMessage("Regeneration rate for " + reason + " set to " + value);
            } else {
                sender.sendMessage("Invalid regeneration reason.");
            }
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
            durabilityModifier = value;
            saveConfigJson();
            sender.sendMessage("Global durability damage multiplier set to " + value + "!");
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid number!");
        }
        return true;
    }

    private boolean handleSpectatorHeads(CommandSender sender, String[] args) {
        if (args[0].equalsIgnoreCase("on")) {
            spectatorHeadsEnabled = true;

            // Shows spectators to other spectators
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        if (target.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                            player.showPlayer(this, target);
                        }
                    }
                }
            }
            sender.sendMessage("Spectator heads are now visible.");
        } else if (args[0].equalsIgnoreCase("off")) {
            spectatorHeadsEnabled = false;

            // Hides spectators from other spectators
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        if (target.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                            player.hidePlayer(this, target);
                        }
                    }
                }
            }
            sender.sendMessage("Spectator heads are now hidden.");
        } else {
            sender.sendMessage("Usage: /spectatorHeads <on|off>");
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

    public Map<String, Double> getRegenModifiers() {
        return regenModifiers;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getDurabilityModifier() {
        return durabilityModifier;
    }

    public boolean getSpectatorHeadsEnabled() {
        return spectatorHeadsEnabled;
    }
}