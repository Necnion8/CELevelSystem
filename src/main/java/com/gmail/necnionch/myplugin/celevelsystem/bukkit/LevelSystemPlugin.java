package com.gmail.necnionch.myplugin.celevelsystem.bukkit;

import com.gmail.necnionch.myplugin.celevelsystem.bukkit.config.Level;
import com.gmail.necnionch.myplugin.celevelsystem.bukkit.config.MainConfig;
import com.gmail.necnionch.myplugin.n8chatcaster.bukkit.events.ChatFormatProcessEvent;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;

public final class LevelSystemPlugin extends JavaPlugin implements Listener {
    private final MainConfig mainConfig = new MainConfig(this);

    @Override
    public void onEnable() {
        mainConfig.load();
        getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("celvlsys")
                .withPermission("celevelsystem.command.celvlsys")
                .withSubcommand(new CommandAPICommand("setlevel")
                        .withArguments(new EntitySelectorArgument<Collection<Player>>("players", EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("level"))
                        .executes(this::execSetLevel)
                )
                .withSubcommand(new CommandAPICommand("getlevel")
                        .withArguments(new EntitySelectorArgument<Player>("players", EntitySelector.ONE_PLAYER))
                        .executes(this::execGetLevel)
                )
                .withSubcommand(new CommandAPICommand("addscore")
                        .withArguments(new EntitySelectorArgument<Collection<Player>>("players", EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("value"))
                        .executes(this::execAddScore)
                )
                .withSubcommand(new CommandAPICommand("getscore")
                        .withArguments(new EntitySelectorArgument<Player>("players", EntitySelector.ONE_PLAYER))
                        .executes(this::execGetScore)
                )
                .withSubcommand(new CommandAPICommand("reload")
                        .executes(this::execReload)
                )
                .register();

    }

    @Override
    public void onDisable() {
        mainConfig.save();
    }


    @EventHandler
    public void onChatFormat(ChatFormatProcessEvent event) {
        Player player = event.getPlayer();
        int level = mainConfig.getPlayerLevel(player.getUniqueId());
        Level setting = mainConfig.getLevelConfigByLevel(level);

        String formatted = "";
        if (setting != null) {
            formatted = ChatColor.translateAlternateColorCodes('&', setting.getChatFormat().replace("{level}", "" + level));
        }
        event.getValues().put("levelsystem", formatted);
    }

    public int remainingScore(int currentLevel) {
        double modifier = mainConfig.getScoreToLevelModifier();
        return Math.max(mainConfig.getLevelBaseScore(), (int) (currentLevel * modifier));
    }

    public void changeScore(Player player, int added) {
        int level = mainConfig.getPlayerLevel(player.getUniqueId());
        int remaining = mainConfig.getPlayerRemaining(player.getUniqueId());
        int newRemaining = remaining - added;

        while (newRemaining <= 0) {
            level++;
            newRemaining = remainingScore(level) + remaining;
        }
        mainConfig.setPlayerLevel(player.getUniqueId(), level);
        mainConfig.setPlayerRemaining(player.getUniqueId(), newRemaining);
    }

    private int execSetLevel(CommandSender sender, Object[] objects) {
        @SuppressWarnings("unchecked") List<Player> players = (List<Player>) objects[0];
        int level = (int) objects[1];

        players.forEach(player -> {
            mainConfig.setPlayerLevel(player.getUniqueId(), level);
            mainConfig.setPlayerRemaining(player.getUniqueId(), remainingScore(level));
        });
        sender.sendMessage(ChatColor.GOLD + "レベルを変更しました");
        return 0;
    }

    private int execGetLevel(CommandSender sender, Object[] objects) {
        Player player = (Player) objects[0];
        int level = mainConfig.getPlayerLevel(player.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + player.getName() + " のレベルは " + level + " です");
        return level;
    }

    private int execAddScore(CommandSender sender, Object[] objects) {
        @SuppressWarnings("unchecked") List<Player> players = (List<Player>) objects[0];
        int score = (int) objects[1];

        players.forEach(player -> {
            changeScore(player, score);
        });
        sender.sendMessage(ChatColor.GOLD + "スコアを設定しました");
        return 0;
    }

    private int execGetScore(CommandSender sender, Object[] objects) {
        Player player = (Player) objects[0];
        int score = mainConfig.getPlayerRemaining(player.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + player.getName() + " のスコアは " + score + " です");
        return score;
    }

    private int execReload(CommandSender sender, Object[] objects) {
        mainConfig.load();
        sender.sendMessage(ChatColor.GOLD + "設定ファイルを再読み込みしました");
        return 0;
    }


}
