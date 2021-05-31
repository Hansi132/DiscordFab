package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import org.jetbrains.annotations.NotNull;
import org.kilocraft.essentials.api.event.EventHandler;
import org.kilocraft.essentials.api.event.player.PlayerMutedEvent;

public class MuteListener implements EventHandler<PlayerMutedEvent> {
    @Override
    public void handle(@NotNull PlayerMutedEvent playerMutedEvent) {
        DiscordFab.getInstance().getChatSynchronizer().onUserPunished(playerMutedEvent);
    }
}
