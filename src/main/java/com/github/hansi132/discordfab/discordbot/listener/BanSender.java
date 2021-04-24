package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import net.dv8tion.jda.api.EmbedBuilder;
import org.kilocraft.essentials.api.event.player.PlayerBannedEvent;

import java.awt.*;
import java.util.Date;
import java.util.Objects;

public class BanSender {
    public static void sendBan(PlayerBannedEvent playerBannedEvent) {
        String victimName = playerBannedEvent.getVictim().getName();
        String staffName = playerBannedEvent.getSource().getName();
        String reason = playerBannedEvent.getReason();
        long expiry = playerBannedEvent.getExpiry();
        boolean ipBan = playerBannedEvent.isIpBan();
        boolean permanentBan = playerBannedEvent.isPermanent();
        boolean silent = playerBannedEvent.isSilent();

        Color color = permanentBan ? Color.red : Color.orange;
        String title = (permanentBan ? "Permanent ban" : "Temp ban") +
                (ipBan ? " ipBan" : "") +
                (silent ? " performed silent" : "");

        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(color)
            .setTitle(title)
            .addField(staffName + " banned: " , victimName, false)
            .addField("Reason:", reason, false);
            if (permanentBan) {
                embedBuilder.addField("Time expires:", "Never", false);
            } else {
                embedBuilder.addField("Time expires:", new Date(expiry).toString(), false);
            }

        Objects.requireNonNull(DiscordFab.getBot().getTextChannelById(DiscordFab.getInstance().getConfig().banSenderChat)).sendMessage(embedBuilder.build()).queue();


    }

}
