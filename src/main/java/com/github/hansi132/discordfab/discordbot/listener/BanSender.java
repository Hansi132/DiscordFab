package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import net.dv8tion.jda.api.EmbedBuilder;
import org.kilocraft.essentials.api.event.player.PlayerBannedEvent;

import java.awt.*;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class BanSender {
    public static void sendBan(PlayerBannedEvent playerBannedEvent) {
        String victimName = playerBannedEvent.getVictim().getName();
        String staffName = playerBannedEvent.getSource().getName();
        String reason = playerBannedEvent.getReason();
        Date expiry = new Date(playerBannedEvent.getExpiry());
        boolean ipBan = playerBannedEvent.isIpBan();
        boolean permanentBan = playerBannedEvent.isPermanent();
        boolean silent = playerBannedEvent.isSilent();

        Color color = permanentBan ? Color.red : Color.orange;
        String title = (permanentBan ? "Permanent ban" : "Temp ban") +
                (ipBan ? " ipBan" : "") +
                (silent ? " performed silent" : "");

        Date from = new Date();
        int banTimeInMillis = (int) Math.abs(expiry.getTime() - from.getTime());
        long diff = TimeUnit.HOURS.convert(banTimeInMillis, TimeUnit.MILLISECONDS) + 1;
        String banTimeText = diff + " hours";

        if (diff >= 24) {
            diff = diff / 24;
            banTimeText = diff + " days";
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(color)
            .setTitle(title)
            .addField(staffName + " banned: " , victimName, false)
            .addField("Reason:", reason, false);
            if (permanentBan) {
                embedBuilder.addField("Time expires:", "Never", false);
            } else {
                embedBuilder.addField("Time expires:", expiry.toString(), false);
                embedBuilder.addField("Ban time:", banTimeText, false);
            }


        Objects.requireNonNull(DiscordFab.getBot().getTextChannelById(DiscordFab.getInstance().getConfig().banSenderChat)).sendMessage(embedBuilder.build()).queue();


    }

}
