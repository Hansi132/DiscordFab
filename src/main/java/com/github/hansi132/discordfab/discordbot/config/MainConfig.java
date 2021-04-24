package com.github.hansi132.discordfab.discordbot.config;

import com.github.hansi132.discordfab.discordbot.config.section.BotPretenseConfigSection;
import com.github.hansi132.discordfab.discordbot.config.section.DefaultEmbedConfigSection;
import com.github.hansi132.discordfab.discordbot.config.section.MessagesConfigSection;
import com.github.hansi132.discordfab.discordbot.config.section.UserSyncConfigSection;
import com.github.hansi132.discordfab.discordbot.config.section.messagesync.ChatSynchronizerConfigSection;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MainConfig {
    public static final String HEADER = "DiscordFab! Main Configuration File\n" +
            "Licensed Under the MIT License, Copyright (c) 2020 KiloCraft\n" +
            "DiscordFab is using HOCON for its configuration files\n learn more about it here: " +
            "https://docs.spongepowered.org/stable/en/server/getting-started/configuration/hocon.html" +
            "\nYou can use Color Codes in string parameters, the character is \"&\" " +
            "More info at: https://minecraft.tools/en/color-code.php \ne.g: \"&eThe Yellow Thing\" will be yellow";

    @Setting(value = "prefix", comment = "The prefix you use for the commands, Default = \"k!\"")
    public String prefix = "k!";

    @Setting(value = "serverIp", comment = "The IP of the Minecraft Server")
    public String serverIp = "50kilo.org";

    @Setting
    public String serverUserName = "Server";

    @Setting
    public String serverUserAvatarUrl = "";

    @Setting
    public ChatSynchronizerConfigSection chatSynchronizer = new ChatSynchronizerConfigSection();

    @Setting
    public DefaultEmbedConfigSection defaultEmbed = new DefaultEmbedConfigSection();

    @Setting
    public MessagesConfigSection messages = new MessagesConfigSection();

    @Setting
    public UserSyncConfigSection userSync = new UserSyncConfigSection();

    @Setting
    public BotPretenseConfigSection activity = new BotPretenseConfigSection();

    @Setting
    public Long suggestionChatId = 829822701254082621L;

    @Setting
    public Long suggestionAdminChatId = 830791613176741890L;

    @Setting
    public Long banSenderChat = 530489915964653580L;

}
