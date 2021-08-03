package com.github.hansi132.discordfab.discordbot.config.section.messagesync;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ChatSynchronizerConfigSection {

    @Setting(value = "toDiscord", comment = "Enables chat synchronization to Discord")
    public boolean toDiscord = true;

    @Setting(value = "toMinecraft", comment = "Enables chat synchronization to Minecraft")
    public boolean toMinecraft = true;


    @Setting(value = "messages")
    public ChatSyncMessagesConfigSection messages = new ChatSyncMessagesConfigSection();

    @Setting("minecraftAvatarRenderOptions")
    public MCAvatarRenderOptionsConfigSection renderOptions = new MCAvatarRenderOptionsConfigSection();

    @Setting(value = "publicChat", comment = "for public chat")
    public ChatChannelSynchronizerConfigSection publicChat = new ChatChannelSynchronizerConfigSection("&7@&f%name% &8»&r");

    @Setting(value = "staffChat", comment = "for staff chat")
    public ChatChannelSynchronizerConfigSection staffChat = new ChatChannelSynchronizerConfigSection("&8[&cStaff&8] &7@&f%name% &8»&r");

    @Setting(value = "builderChat", comment = "for builder chat")
    public ChatChannelSynchronizerConfigSection builderChat = new ChatChannelSynchronizerConfigSection("&1[&aBuilder&1] &7@&f%name% &8»&r");

    @Setting(value = "socialSpy", comment = "for public chat")
    public ChatChannelSynchronizerConfigSection socialSpy = new ChatChannelSynchronizerConfigSection("**to %target% ->**");

    @Setting(value = "suggestionChat", comment = "for suggestion chat")
    public SuggestionChannelConfigSection suggestionChat = new SuggestionChannelConfigSection();

    @Setting(value = "banChatId", comment = "for ban reports")
    public long banChatId = 0;

    @Setting
    public EventMessagesConfigSection event = new EventMessagesConfigSection();

    public boolean isEnabled() {
        return toDiscord || toMinecraft;
    }
}
