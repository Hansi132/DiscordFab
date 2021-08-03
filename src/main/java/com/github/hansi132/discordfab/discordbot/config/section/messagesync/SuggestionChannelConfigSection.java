package com.github.hansi132.discordfab.discordbot.config.section.messagesync;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class SuggestionChannelConfigSection {

    @Setting
    public boolean enabled = false;

    @Setting
    public long discordChannelId = 0L;

    @Setting
    public long staffChannelId = 0L;

    @Setting
    public String upvoteReactionId = ":thumbup:";

    @Setting
    public String downvoteReactionId = ":thumbdown:";

}
