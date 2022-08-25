package com.gmail.necnionch.myplugin.celevelsystem.bukkit.config;

import com.gmail.necnionch.myplugin.celevelsystem.bukkit.group.Group;
import com.gmail.necnionch.myplugin.celevelsystem.common.BukkitConfigDriver;
import com.google.common.collect.Maps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MainConfig extends BukkitConfigDriver {
    private final JavaPlugin plugin;
    private @Nullable BukkitTask saveTask;
    private final Map<String, Group> groups = Maps.newHashMap();
    private final Map<UUID, Integer> playerScores = Maps.newHashMap();

    public MainConfig(JavaPlugin plugin) {
        super(plugin, "config.yml", "empty.yml");
        this.plugin = plugin;
    }

    @Override
    public boolean onLoaded(FileConfiguration config) {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        if (super.onLoaded(config)) {
            groups.clear();
            playerScores.clear();
            ConfigurationSection section = config.getConfigurationSection("groups");
            if (section != null) {
                for (String groupName : section.getKeys(false)) {
                    int value = section.getInt(groupName + ".value");
                    groups.put(groupName, new Group(groupName, value));
                }
            }

            section = config.getConfigurationSection("scores");
            if (section != null) {
                for (String uuidStr : section.getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid UUID: " + uuidStr);
                        continue;
                    }

                    int score = section.getInt(uuidStr);
                    playerScores.put(uuid, score);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean save() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        config = new YamlConfiguration();
        groups.values().forEach(group -> {
            config.set("groups." + group.getGroupName() + ".value", group.getValue());
        });
        playerScores.forEach((uuid, score) -> {
            config.set("scores." + uuid.toString(), score);
        });
        return super.save();
    }

    public Map<String, Group> groups() {
        return groups;
    }

    public @Nullable Group getGroupByValue(int value) {
        List<Group> sorted = groups.values().stream().sorted(Comparator.comparingInt(Group::getValue).reversed()).collect(Collectors.toList());
        for (Group group : sorted) {
            if (group.getValue() <= value)
                return group;
        }
        return null;
    }

    public @Nullable Group getGroupByName(String groupName) {
        return groups.get(groupName);
    }


    public int getPlayerScore(UUID player) {
        return playerScores.getOrDefault(player, 0);
    }

    public int setPlayerScore(UUID player, int score) {
        playerScores.put(player, score);
        queue();
        return score;
    }

    public int addPlayerScore(UUID player, int score) {
        int newValue = Math.max(0, playerScores.getOrDefault(player, 0)) + Math.max(0, score);
        playerScores.put(player, newValue);
        queue();
        return newValue;
    }

    public int resetPlayerScore(UUID player) {
        playerScores.remove(player);
        queue();
        return 0;
    }

    public Map<UUID, Integer> playerScores() {
        return playerScores;
    }

    private void queue() {
        if (saveTask != null)
            saveTask.cancel();

        saveTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::save, 1 * 60 * 20);
    }



}
