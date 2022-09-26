package com.gmail.necnionch.myplugin.celevelsystem.bukkit.config;

import com.gmail.necnionch.myplugin.celevelsystem.common.BukkitConfigDriver;
import com.google.common.collect.Lists;
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

public class MainConfig extends BukkitConfigDriver {
    private final JavaPlugin plugin;
    private @Nullable BukkitTask saveTask;
//    private final Map<String, Group> groups = Maps.newHashMap();
    private final Map<UUID, Integer> playerRemainingScore = Maps.newHashMap();
    private final Map<UUID, Integer> playerLevels = Maps.newHashMap();
    private final List<Level> levels = Lists.newArrayList();
    private int levelBaseScore = 3;
    private double scoreToLevelModifier = 1.5;

    public MainConfig(JavaPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public boolean onLoaded(FileConfiguration config) {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        if (super.onLoaded(config)) {
            playerRemainingScore.clear();
            playerLevels.clear();
            levels.clear();
            scoreToLevelModifier = config.getDouble("score-to-level-modifier", 1.5);
            levelBaseScore = config.getInt("level-base-score", 3);

            ConfigurationSection section = config.getConfigurationSection("players");
            if (section != null) {
                for (String uuidStr : section.getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid UUID: " + uuidStr);
                        continue;
                    }

                    int remaining = section.getInt(uuidStr + ".remaining", 0);
                    int level = section.getInt(uuidStr + ".level", 0);
                    playerRemainingScore.put(uuid, remaining);
                    playerLevels.put(uuid, level);
                }
            }

            section = config.getConfigurationSection("levels");
            if (section != null) {
                for (String levelStr : section.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelStr);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    Level levelEntry = new Level(level, section.getString(levelStr + ".format", ""));
                    levels.add(levelEntry);
                }
                levels.sort(Comparator.comparingInt(Level::getLevel).reversed());
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

        config.set("score-to-level-modifier", scoreToLevelModifier);
        config.set("level-base-score", levelBaseScore);

        playerRemainingScore.forEach((uuid, score) -> {
            config.set("players." + uuid.toString() + ".remaining", score);
        });
        playerLevels.forEach((uuid, level) -> {
            config.set("players." + uuid.toString() + ".level", level);
        });
        levels.forEach((level) -> {
            config.set("levels." + level.getLevel() + ".format", level.getChatFormat());
        });
        return super.save();
    }


    public int getPlayerRemaining(UUID player) {
        return playerRemainingScore.getOrDefault(player, 0);
    }

    public int setPlayerRemaining(UUID player, int score) {
        playerRemainingScore.put(player, score);
        queue();
        return score;
    }

    public int getPlayerLevel(UUID player) {
        return Math.max(1, playerLevels.getOrDefault(player, 1));
    }

    public int setPlayerLevel(UUID player, int level) {
        playerLevels.put(player, level);
        queue();
        return level;
    }

    public Map<UUID, Integer> playerScores() {
        return playerRemainingScore;
    }

    public Map<UUID, Integer> playerLevels() {
        return playerLevels;
    }


    public double getScoreToLevelModifier() {
        return scoreToLevelModifier;
    }

    public void setScoreToLevelModifier(double modifier) {
        this.scoreToLevelModifier = modifier;
        save();
    }

    public int getLevelBaseScore() {
        return levelBaseScore;
    }

    public void setLevelBaseScore(int levelBaseScore) {
        this.levelBaseScore = levelBaseScore;
        save();
    }

    public @Nullable Level getLevelConfigByLevel(int level) {
        for (Level levelEntry : levels) {
            if (levelEntry.getLevel() <= level)
                return levelEntry;
        }
        return null;
    }

    private void queue() {
        if (saveTask != null)
            saveTask.cancel();

        saveTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::save, 1 * 60 * 20);
    }



}
