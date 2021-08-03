package com.github.hansi132.discordfab.discordbot.util;

import com.github.hansi132.discordfab.DiscordFab;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class FabUtil {

    private static final DiscordFab DISCORD_FAB = DiscordFab.getInstance();

    public static UUID uuidFromShortenedUuidString(@NotNull final String id) {
        return new UUID(
                new BigInteger(id.substring(0, 16), 16).longValue(),
                new BigInteger(id.substring(16), 16).longValue()
        );
    }

    @Nullable
    public static Activity.ActivityType activityTypeFromString(@NotNull final String string) {
        Activity.ActivityType type = null;
        for (Activity.ActivityType value : Activity.ActivityType.values()) {
            if (value.name().equalsIgnoreCase(string)) {
                type = value;
                break;
            }
        }

        return type == Activity.ActivityType.CUSTOM_STATUS ? null : type;
    }

    @Nullable
    public static OnlineStatus onlineStatusFromString(@NotNull final String string) {
        OnlineStatus status = null;
        for (OnlineStatus value : OnlineStatus.values()) {
            if (value.name().equalsIgnoreCase(string)) {
                status = value;
                break;
            }
        }

        return status == OnlineStatus.UNKNOWN ? null : status;
    }

    public static EmbedBuilder updateFooter(MessageEmbed embed, String footer) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(embed.getTitle());
        builder.setColor(embed.getColor());
        builder.setThumbnail(embed.getThumbnail().getUrl());
        for (MessageEmbed.Field f : embed.getFields()) { builder.addField(f); }
        builder.setFooter(footer);
        return builder;
    }

    public static EmbedBuilder updateEmbed(MessageEmbed embed, Color color, String title) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(title);
        builder.setColor(color);
        builder.setThumbnail(embed.getThumbnail().getUrl());
        builder.setFooter(embed.getFooter().getText());
        for (MessageEmbed.Field f : embed.getFields()) { builder.addField(f); }
        return builder;
    }

    @Nullable
    public static Message getSuggestionMessage(long staffMessageId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            String selectSql = "SELECT * FROM trackedsuggestions WHERE StaffMessageId = ?;";
            PreparedStatement insertStatement = connection.prepareStatement(selectSql);
            insertStatement.setLong(1, staffMessageId);
            ResultSet rs = insertStatement.executeQuery();
            if (rs.next()) {
                long suggestionMessageId = rs.getLong("SuggestionMessageId");
                TextChannel suggestionChannel = DISCORD_FAB.getGuild().getTextChannelById(DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.discordChannelId);
                return suggestionChannel.retrieveMessageById(suggestionMessageId).complete();

            } else {
                DiscordFab.LOGGER.error("Could not find message in database!");
            }
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
        }
        return null;
    }
    
    
}
