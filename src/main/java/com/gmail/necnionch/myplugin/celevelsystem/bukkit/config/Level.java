package com.gmail.necnionch.myplugin.celevelsystem.bukkit.config;

public class Level {

    private final int level;
    private final String chatFormat;

    public Level(int level, String chatFormat) {
        this.level = level;
        this.chatFormat = chatFormat;
    }

    public int getLevel() {
        return level;
    }

    public String getChatFormat() {
        return chatFormat;
    }

}
