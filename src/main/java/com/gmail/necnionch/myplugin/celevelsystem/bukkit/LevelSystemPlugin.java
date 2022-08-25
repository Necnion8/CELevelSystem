package com.gmail.necnionch.myplugin.celevelsystem.bukkit;

import com.gmail.necnionch.myplugin.celevelsystem.bukkit.config.MainConfig;
import com.gmail.necnionch.myplugin.celevelsystem.bukkit.group.Group;
import com.google.common.collect.Sets;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.util.Tristate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class LevelSystemPlugin extends JavaPlugin {
    private LuckPerms luckPerms;
    private final MainConfig mainConfig = new MainConfig(this);

    @Override
    public void onEnable() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().warning("Failed to hook to LuckPerms Service");
            setEnabled(false);
            return;
        }

        luckPerms = provider.getProvider();
        mainConfig.load();


        new CommandAPICommand("celvlsys")
                .withPermission("celevelsystem.command.celvlsys")
                .withSubcommand(new CommandAPICommand("setgroup")
                        .withArguments(new StringArgument("lpGroup"))
                        .withArguments(new IntegerArgument("scoreValue"))
                        .executes(this::execSetGroup)
                )
                .withSubcommand(new CommandAPICommand("removegroup")
                        .withArguments(new StringArgument("lpGroup"))
                        .executes(this::execRemoveGroup)
                )
                .withSubcommand(new CommandAPICommand("set")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("value"))
                        .executes(this::execSetScore)
                )
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .executes(this::execAddScore)
                )
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("value"))
                        .executes(this::execAddScore)
                )
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .executes(this::execRemoveScore)
                )
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("value"))
                        .executes(this::execRemoveScore)
                )
                .withSubcommand(new CommandAPICommand("reset")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .executes(this::execResetScore)
                )
                .withSubcommand(new CommandAPICommand("get")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
                        .executes(this::execGetScore)
                )
                .register();

    }

    @Override
    public void onDisable() {
        mainConfig.save();
    }

    public void changeScore(Player player, int oldScore, int newScore) {
        Group group = mainConfig.getGroupByValue(newScore);

        net.luckperms.api.model.group.Group tmpGroup = null;
        if (group != null) {
            tmpGroup = luckPerms.getGroupManager().getGroup(group.getGroupName());
            if (tmpGroup == null) {
                getLogger().warning("Not found LuckPerms Group: " + group.getGroupName());
                return;
            }
        }
        net.luckperms.api.model.group.Group lpGroup = tmpGroup;

        User u = luckPerms.getUserManager().getUser(player.getUniqueId());
        CompletableFuture<User> f;
        if (u == null) {
            f = luckPerms.getUserManager().loadUser(player.getUniqueId());
        } else {
            f = CompletableFuture.completedFuture(u);
        }

        f.whenComplete((user, error) -> {
            if (error != null) {
                error.printStackTrace();

            } else if (user != null) {
                Set<String> removingGroups = Sets.newHashSet(mainConfig.groups().keySet());
                NodeMap nodeMap = user.data();

                if (group != null) {
                    InheritanceNode targetNode = InheritanceNode.builder(lpGroup).build();
                    if (!Tristate.TRUE.equals(nodeMap.contains(targetNode, NodeEqualityPredicate.EXACT))) {
                        getLogger().info("Add " + group.getGroupName() + " group to " + user.getUsername() + " (score: " + newScore + ")");
                        nodeMap.add(targetNode);
                        removingGroups.remove(group.getGroupName());
                    }
                }

                removingGroups.forEach(groupName -> {
                    InheritanceNode node = InheritanceNode.builder(groupName).build();
                    nodeMap.remove(node);
                });

                luckPerms.getUserManager().saveUser(user);
            }
        });
    }


    private int execSetGroup(CommandSender sender, Object[] objects) {
        String groupName = (String) objects[0];
        int score = (int) objects[1];

        groupName = groupName.toLowerCase(Locale.ROOT);
        Group group = mainConfig.getGroupByName(groupName);

        if (group == null) {
            group = new Group(groupName, score);
            mainConfig.groups().put(groupName, group);
        } else {
            group.setValue(score);
        }

        mainConfig.save();
        sender.sendMessage(ChatColor.GOLD + "グループ " + groupName + " (score: " + score + ") を追加しました");
        return 0;
    }

    private int execRemoveGroup(CommandSender sender, Object[] objects) {
        String groupName = (String) objects[0];

        groupName = groupName.toLowerCase(Locale.ROOT);
        Group group = mainConfig.getGroupByName(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "そのグループは設定されていません");
        } else {
            mainConfig.groups().remove(groupName);
            mainConfig.save();
            sender.sendMessage(ChatColor.GOLD + "グループ " + groupName + " を削除しました");
        }
        return 0;
    }

    private int execSetScore(CommandSender sender, Object[] objects) {
        @SuppressWarnings("unchecked") List<Player> players = (List<Player>) objects[0];
        int score = (int) objects[1];

        players.forEach(player -> {
            int oldScore = mainConfig.getPlayerScore(player.getUniqueId());
            int newScore = mainConfig.setPlayerScore(player.getUniqueId(), score);
            changeScore(player, oldScore, newScore);
        });
        sender.sendMessage(ChatColor.GOLD + "レベルスコアを変更しました");
        return 0;
    }
    private int execAddScore(CommandSender sender, Object[] objects) {
        @SuppressWarnings("unchecked") List<Player> players = (List<Player>) objects[0];
        int score = objects.length >= 2 ? (int) objects[1] : 1;

        players.forEach(player -> {
            int newScore = mainConfig.addPlayerScore(player.getUniqueId(), score);
            changeScore(player, mainConfig.getPlayerScore(player.getUniqueId()), newScore);
        });
        sender.sendMessage(ChatColor.GOLD + "レベルスコアを変更しました");
        return 0;
    }

    private int execRemoveScore(CommandSender sender, Object[] objects) {
        @SuppressWarnings("unchecked") List<Player> players = (List<Player>) objects[0];
        int score = objects.length >= 2 ? (int) objects[1] : 1;

        players.forEach(player -> {
            int oldScore = mainConfig.getPlayerScore(player.getUniqueId());
            int newScore = Math.max(0, oldScore - score);
            newScore = mainConfig.setPlayerScore(player.getUniqueId(), newScore);
            changeScore(player, oldScore, newScore);
        });
        sender.sendMessage(ChatColor.GOLD + "レベルスコアを変更しました");
        return 0;
    }

    private int execGetScore(CommandSender sender, Object[] objects) {
        Player player = (Player) objects[0];

        int score = mainConfig.getPlayerScore(player.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + player.getName() + " のレベルスコアは " + score + " です");
        return score;
    }

    private int execResetScore(CommandSender sender, Object[] objects) {
        @SuppressWarnings("unchecked") List<Player> players = (List<Player>) objects[0];

        players.forEach(player -> {
            int score = mainConfig.getPlayerScore(player.getUniqueId());
            int newScore = mainConfig.resetPlayerScore(player.getUniqueId());
            changeScore(player, score, newScore);
        });
        sender.sendMessage(ChatColor.GOLD + "レベルスコアをリセットしました");
        return 0;
    }


}
