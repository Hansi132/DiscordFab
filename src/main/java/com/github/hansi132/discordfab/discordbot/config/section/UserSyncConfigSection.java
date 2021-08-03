package com.github.hansi132.discordfab.discordbot.config.section;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;

@ConfigSerializable
public class UserSyncConfigSection {

    @Setting(value = "linkedRoleName", comment = "ID of the Linked Role")
    public long linkedRoleId = 123456789101112131L;

    @Setting(value = "syncedRoles", comment = "List of Role IDs that should be synced")
    public ArrayList<Long> syncedRoles = new ArrayList<>();

    @Setting(value = "mutedRoleId", comment = "ID of the Muted Role")
    public long mutedRoleId = 123456789101112132L;

    @Setting(value = "command", comment = "Command that should get executed when a player links")
    public String command = "scoreboard players add %player% voted 3";

    @Setting(value = "syncDisplayName", comment = "Syncs the in-game display name with discord")
    public boolean syncDisplayName = true;

}
