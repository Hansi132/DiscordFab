package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import org.jetbrains.annotations.NotNull;
import org.kilocraft.essentials.api.event.EventHandler;
import org.kilocraft.essentials.api.event.player.PlayerBannedEvent;

public class BanListener implements EventHandler<PlayerBannedEvent> {
    @Override
    public void handle(@NotNull PlayerBannedEvent playerBannedEvent) {
        if (!playerBannedEvent.isSilent()) DiscordFab.getInstance().getChatSynchronizer().onUserPunished(playerBannedEvent.getVictim(), playerBannedEvent.getSource(), playerBannedEvent.getReason(), false);
    }
}

