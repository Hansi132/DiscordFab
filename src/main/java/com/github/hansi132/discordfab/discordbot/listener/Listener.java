package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.ChatSynchronizer;
import com.github.hansi132.discordfab.discordbot.api.command.BotCommandSource;
import com.github.hansi132.discordfab.discordbot.integration.UserSynchronizer;
import com.github.hansi132.discordfab.discordbot.util.Constants;
import com.github.hansi132.discordfab.discordbot.util.DatabaseConnection;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

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
        } else if (event.getChannel().getIdLong() == DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.discordChannelId) {
            DISCORD_FAB.getChatSynchronizer().onSuggestion(event);
        } else if (!event.isWebhookMessage() && DISCORD_FAB.getConfig().chatSynchronizer.toMinecraft) {
            DISCORD_FAB.getChatSynchronizer().onDiscordChat(
                event.getChannel(), Objects.requireNonNull(event.getMember()), event.getMessage()
            );
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
