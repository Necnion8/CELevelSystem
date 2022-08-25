package com.gmail.necnionch.myplugin.celevelsystem.bukkit.group;

public class Group {

    private final String groupName;
    private int value;

    public Group(String groupName, int value) {
        this.groupName = groupName;
        this.value = value;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}
