package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.ChatSynchronizer;
import com.github.hansi132.discordfab.discordbot.api.command.BotCommandSource;
import com.github.hansi132.discordfab.discordbot.integration.UserSynchronizer;
import com.github.hansi132.discordfab.discordbot.util.Constants;
import com.github.hansi132.discordfab.discordbot.util.DatabaseConnection;
import com.github.hansi132.discordfab.discordbot.util.FabUtil;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DiscordFab DISCORD_FAB = DiscordFab.getInstance();

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        DiscordFab.getInstance().getInviteTracker().cacheInvites(DiscordFab.getInstance().getGuild());
        LOGGER.info("{} is ready", event.getJDA().getSelfUser().getAsTag());
        try {
            Connection connection = DatabaseConnection.getConnection();
            Statement stmt = connection.createStatement();
            stmt.execute(Constants.linkedAccountsDatabase);
            stmt.execute(Constants.trackedinvitesDatabase);
            stmt.execute(Constants.trackedSuggestions);
            if (DISCORD_FAB.isDevelopment()) {
                LOGGER.info("First Database Connection successfully established");
            }
        } catch (SQLException e) {
            LOGGER.fatal("Could not connect to the Database!", e);
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        final String raw = event.getMessage().getContentRaw();
        if (event.isFromType(ChannelType.PRIVATE) && !event.getAuthor().isBot()) {
            if (!UserSynchronizer.isLinkCode(raw)) {
                event.getPrivateChannel().sendMessage(DISCORD_FAB.getConfig().messages.invalid_link_key).queue();
                return;
            }

            UserSynchronizer.sync(event.getPrivateChannel(),null, event.getAuthor(), UserSynchronizer.getLinkCode(raw));
        }
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        final User user = event.getAuthor();
        if (user.isBot()) {
            return;
        }

        final String raw = event.getMessage().getContentDisplay();
        final String prefix = DiscordFab.getInstance().getConfig().prefix;

        if (
                !event.isWebhookMessage() &&
                        !raw.equalsIgnoreCase(prefix) &&
                        raw.toLowerCase().startsWith(prefix.toLowerCase()) &&
                        ChatSynchronizer.shouldRespondToCommandIn(event.getChannel().getIdLong())
        ) {
            DISCORD_FAB.getCommandManager().execute(
                    new BotCommandSource(
                            event.getJDA(), event.getGuild(), event.getChannel(), user, event.getMember(), event
                    ),
                    raw
            );
        } else if (event.getChannel().getIdLong() == DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.discordChannelId && DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.enabled) {
            DISCORD_FAB.getChatSynchronizer().onSuggestion(event);
        } else if (event.getChannel().getIdLong() == DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.staffChannelId) {
            DISCORD_FAB.getChatSynchronizer().onStaffInput(event);
        } else if (!event.isWebhookMessage() && DISCORD_FAB.getConfig().chatSynchronizer.toMinecraft) {
            DISCORD_FAB.getChatSynchronizer().onDiscordChat(
                event.getChannel(), Objects.requireNonNull(event.getMember()), event.getMessage()
            );
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        Message message = event.getMessage();
        if (message == null) {
            event.reply("Couldn't find associated message").queue();
            return;
        }
        String id = event.getButton().getId();
        Color color;
        String title;
        switch (id) {
            case "discordfab-accept" -> {
                color = Color.GREEN;
                title = "Accepted Suggestion";
            }
            case "discordfab-deny" -> {
                color = Color.RED;
                title = "Denied Suggestion";
            }
            default -> {
                event.reply("Unknown button").queue();
                return;
            }
        }
        Message suggestionMessage = FabUtil.getSuggestionMessage(message.getIdLong());
        if (suggestionMessage == null) {
            event.reply("Unable to find original message").queue();
            return;
        }
        try {
            EmbedBuilder builder = FabUtil.updateEmbed(suggestionMessage.getEmbeds().get(0), color, title);
            message.editMessageEmbeds(builder.build()).queue();
            suggestionMessage.editMessageEmbeds(builder.build()).queue();
            event.reply("Successfully updated suggestion").queue();
        } catch (Exception e) {
            event.reply(e.getMessage()).queue();
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        DISCORD_FAB.getInviteTracker().onGuildMemberJoin(event);
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        DISCORD_FAB.getInviteTracker().onGuildInviteChange(event);
    }

    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        DISCORD_FAB.getInviteTracker().onGuildInviteChange(event);
    }
}
