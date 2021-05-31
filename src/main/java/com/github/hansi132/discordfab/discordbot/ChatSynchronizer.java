package com.github.hansi132.discordfab.discordbot;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.config.section.messagesync.ChatChannelSynchronizerConfigSection;
import com.github.hansi132.discordfab.discordbot.config.section.messagesync.ChatSynchronizerConfigSection;
import com.github.hansi132.discordfab.discordbot.integration.UserSynchronizer;
import com.github.hansi132.discordfab.discordbot.util.MinecraftAvatar;
import com.github.hansi132.discordfab.discordbot.util.user.LinkedUser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.event.player.PlayerBannedEvent;
import org.kilocraft.essentials.api.event.player.PlayerMutedEvent;
import org.kilocraft.essentials.api.event.player.PlayerPunishEventInterface;
import org.kilocraft.essentials.api.text.ComponentText;
import org.kilocraft.essentials.api.text.TextFormat;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.user.User;
import org.kilocraft.essentials.api.user.preference.Preference;
import org.kilocraft.essentials.api.util.EntityIdentifiable;
import org.kilocraft.essentials.chat.ServerChat;
import org.kilocraft.essentials.commands.CommandUtils;
import org.kilocraft.essentials.user.preference.Preferences;
import org.kilocraft.essentials.util.text.Texter;

import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatSynchronizer {
    private static final Logger LOGGER = DiscordFab.LOGGER;
    private static final DiscordFab DISCORD_FAB = DiscordFab.getInstance();
    private static final ChatSynchronizerConfigSection CONFIG = DISCORD_FAB.getConfig().chatSynchronizer;
    private static final Pattern SIMPLE_LINK_PATTERN = Pattern.compile("([--:\\w?@%&+~#=]*\\.[a-z]{2,4}/{0,2})((?:[?&](?:\\w+)=(?:\\w+))+|[--:\\w?@%&+~#=]+)?");
    private static final int LINK_MAX_LENGTH = 20;
    private static final String SOCIAL_SPY_ID = "social_spy";
    private final WebhookClientHolder webhookClientHolder;

    public ChatSynchronizer() {
        this.webhookClientHolder = new WebhookClientHolder();
        this.load();
    }

    private static String getMCAvatarURL(@NotNull final UUID uuid) {
        MinecraftAvatar.@Nullable RenderType renderType = MinecraftAvatar.RenderType.getByName(CONFIG.renderOptions.renderType);
        if (renderType == null) {
            renderType = MinecraftAvatar.RenderType.AVATAR;
        }

        return MinecraftAvatar.generateUrl(uuid,
                renderType,
                MinecraftAvatar.RenderType.Model.DEFAULT,
                CONFIG.renderOptions.size,
                CONFIG.renderOptions.scale,
                CONFIG.renderOptions.showOverlay
        );
    }

    public static boolean shouldRespondToCommandIn(final long id) {
        for (MappedChannel value : MappedChannel.values()) {
            if (value.config.discordChannelId == id) {
                return false;
            }
        }

        return true;
    }

    public void load() {
        this.webhookClientHolder.clearAll();
        for (MappedChannel mappedChannel : MappedChannel.values()) {
            this.webhookClientHolder.addClient(mappedChannel.id, mappedChannel.config.webhookUrl);
        }
    }

    @Nullable
    private WebhookClient getClientFor(@NotNull final ServerChat.Channel channel) {
        final MappedChannel mapped = MappedChannel.byServerChannel(channel);
        if (mapped == null) {
            return null;
        }

        return this.webhookClientHolder.getClient(mapped.id);
    }

    public void onSocialSpyWarning(@NotNull final ServerCommandSource source, @NotNull final OnlineUser receiver, @NotNull final String raw, final List<String> marked) {
        final WebhookClient client = this.webhookClientHolder.getClient(MappedChannel.SOCIAL_SPY.id);
        if (client == null) {
            return;
        }

        final WebhookMessageBuilder builder = new WebhookMessageBuilder();
        if (CommandUtils.isPlayer(source)) {
            setMetaFor(KiloServer.getServer().getOnlineUser(source.getName()), builder);
        } else {
            builder.setUsername("Server");
        }

        String string = raw;
        for (String s : marked) {
            Matcher matcher = SIMPLE_LINK_PATTERN.matcher(s);
            if (matcher.matches() && (!s.contains("https://") || !s.contains("http://"))) {
                string.replace(s, "https://" + s);
            }

            string = string.replace(s, CONFIG.socialSpy.sensitiveWordFormat.replace("%word%", s));
        }

        builder.setContent(
                CONFIG.socialSpy.prefix.replace("%source%", source.getName())
                        .replace("%target%", receiver.getName()) + " " + string
        );

        client.send(builder.build());
    }

    public void onGameChat(@NotNull final ServerChat.Channel channel, @NotNull final User user, @NotNull final String message) {
        final WebhookClient client = this.getClientFor(channel);
        if (client == null) {
            return;
        }

        final WebhookMessageBuilder builder = new WebhookMessageBuilder().setContent(
                ComponentText.clearFormatting(message.replaceAll("@", ""))
        );

        setMetaFor(user, builder);
        client.send(builder.build());
    }

    private void sendToGame(@NotNull final MappedChannel mapped, @NotNull final Member member, @NotNull final Text content) {
        if (mapped.channel == null) {
            return;
        }

        MutableText text = ComponentText.toText(mapped.config.prefix
                .replace("%name%", member.getEffectiveName()));
        text.append(" ").append(content);

        mapped.channel.send(text);
    }

    public void onDiscordChat(@NotNull final TextChannel channel,
                              @NotNull final Member member,
                              @NotNull final Message message) {
        final MappedChannel mappedChannel = MappedChannel.byChannelId(channel.getIdLong());
        if (mappedChannel == null || !mappedChannel.isEnabled() || !mappedChannel.toMinecraft || mappedChannel.channel == null) {
            return;
        }

        for (Message.Attachment attachment : message.getAttachments()) {
            MutableText text;
            if (attachment.isImage()) {
                text = new LiteralText("[IMAGE]");
            } else if (attachment.isVideo()) {
                text = new LiteralText("[VIDEO]");
            } else {
                text = new LiteralText("[FILE]");
            }
            MutableText hover = new LiteralText("")
                    .append(new LiteralText("Name: ").formatted(Formatting.GRAY))
                    .append(new LiteralText(attachment.getFileName()).formatted(Formatting.AQUA))
                    .append(new LiteralText("\nResolution: ").formatted(Formatting.GRAY))
                    .append(new LiteralText(attachment.getWidth() + "x" + attachment.getHeight()).formatted(Formatting.AQUA))
                    .append(new LiteralText("\nSize: ").formatted(Formatting.GRAY))
                    .append(new LiteralText(attachment.getSize() / 1024 + "kb").formatted(Formatting.AQUA));
            text.styled(style -> style.withHoverEvent(Texter.Events.onHover(hover)).withClickEvent(Texter.Events.onClickOpen(attachment.getUrl()))).formatted(Formatting.GREEN);
            sendToGame(mappedChannel, member, text);
        }

        String msg = message.getContentRaw();
        if (!msg.equals("")) {
            msg = msg.replaceAll("\\n", "").replaceAll("\\\\n", "");
            Optional<LinkedUser> optional = DiscordFab.getInstance().getUserCache().getByDiscordID(message.getAuthor().getIdLong());
            boolean canUseColors = false;
            if (optional.isPresent()) {
                UUID uuid = optional.get().getMcUUID();
                if (KiloEssentials.getInstance().getPermissionUtil().hasPermission(uuid, "discordfab.colors")) {
                    canUseColors = true;
                }
            }
            if (!canUseColors) msg = ComponentText.clearFormatting(msg);
            sendToGame(mappedChannel, member, ComponentText.toText(msg));
        }
    }

    @Nullable
    private net.dv8tion.jda.api.entities.User getJDAUser(@NotNull final UUID uuid) {
        Optional<LinkedUser> optional = DiscordFab.getInstance().getUserCache().getByUUID(uuid);
        if (optional.isPresent()) {
            LinkedUser linkedUser = optional.get();
            if (linkedUser.getDiscordID().isPresent()) {
                long discordID = linkedUser.getDiscordID().get();
                if (discordID != 0L) {
                    return DiscordFab.getBot().getUserById(discordID);
                }
            }
        }
        return null;
    }

    public void onUserPunished(@NotNull final PlayerPunishEventInterface event) {
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        setMetaFor(event.getVictim(), builder);
        String msg = event instanceof PlayerMutedEvent ? CONFIG.messages.userMuted : CONFIG.messages.userBanned;
        builder.setContent(msg.replace("%victim%", event.getVictim().getName()).replace("%source%", event.getSource().getName()).replace("%reason%", event.getReason()));
        if (!event.isSilent()) this.webhookClientHolder.send(MappedChannel.PUBLIC.id, builder.build());
        net.dv8tion.jda.api.entities.User user = getJDAUser(event.getVictim().getId());
        if (user != null) {
            Guild guild = DISCORD_FAB.getGuild();
            Member member = guild.getMember(user);
            Role role = guild.getRoleById(DISCORD_FAB.getConfig().userSync.mutedRoleId);
            if (role != null) {
                if (member != null) {
                    guild.addRoleToMember(member, role).queue();
                }
            }
        }
        if (event instanceof PlayerBannedEvent) {
            sendReport((PlayerBannedEvent) event);
        }
    }

    public void sendReport(PlayerBannedEvent event)  {
        String victimName = event.getVictim().getName();
        String staffName = event.getSource().getName();
        String reason = event.getReason();
        Date expiry = new Date(event.getExpiry());
        boolean ipBan = event.isIpBan();
        boolean permanentBan = event.isPermanent();
        boolean silent = event.isSilent();

        Color color = permanentBan ? Color.red : Color.orange;
        String title = (permanentBan ? "Permban" : "Tempban") +
                (ipBan ? " ipBan" : "") +
                (silent ? " (silent)" : "");

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


        TextChannel channel = DiscordFab.getBot().getTextChannelById(DiscordFab.getInstance().getConfig().chatSynchronizer.banChatId);
        if (channel != null) channel.sendMessage(embedBuilder.build()).queue();
    }

    public void onUserJoin(@NotNull final User user) {
        try {
            UserSynchronizer.syncRoles(user.getUuid());
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.error("Unexpected error while trying to sync user", e);
        }
        if (user.getPreference(Preferences.VANISH)) return;
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        setMetaFor(user, builder);
        builder.setContent(CONFIG.messages.userJoin.replace("%name%", user.getName()));
        this.webhookClientHolder.send(MappedChannel.PUBLIC.id, builder.build());
    }

    public void onUserLeave(@NotNull final User user) {
        if (user.getPreference(Preferences.VANISH)) return;
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        setMetaFor(user, builder);
        builder.setContent(CONFIG.messages.userLeave.replace("%name%", user.getName()));

        this.webhookClientHolder.send(MappedChannel.PUBLIC.id, builder.build());
    }

    public void broadcast(@NotNull final String message) {
        final WebhookClient client = this.webhookClientHolder.getClient(MappedChannel.PUBLIC.id);
        if (client == null) {
            return;
        }

        final WebhookMessageBuilder builder = new WebhookMessageBuilder().setContent(TextFormat.clearColorCodes(message));
        setServerUserMeta(builder);
        client.send(builder.build());
    }

    public void onSuggestion(GuildMessageReceivedEvent event) {
        String raw = event.getMessage().getContentRaw();
        Member member = event.getMember();
        if (member == null) return;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.green);
        embedBuilder.setTitle("Suggestion");
        embedBuilder.setThumbnail(member.getUser().getAvatarUrl());
        embedBuilder.addField(member.getEffectiveName() + " suggested:", raw, false);
        TextChannel staffChannel = DISCORD_FAB.getGuild().getTextChannelById(DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.staffChannelId);
        if (staffChannel != null) staffChannel.sendMessage(embedBuilder.build());
        embedBuilder.setFooter("Please upvote or downvote.");
        event.getMessage().delete().queue();
        event.getChannel().sendMessage(embedBuilder.build()).queue(message -> {
            message.addReaction(DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.upvoteReactionId).queue();
            message.addReaction(DISCORD_FAB.getConfig().chatSynchronizer.suggestionChat.downvoteReactionId).queue();
        });
    }

    public void broadcast(@NotNull final MessageEmbed message) {
        final TextChannel channel = MappedChannel.PUBLIC.getTextChannel();
        if (channel == null) {
            return;
        }

        channel.sendMessage(message);
    }

    public void setMetaFor(@NotNull final EntityIdentifiable user, @NotNull final WebhookMessageBuilder builder) {
        builder.setAvatarUrl(getMCAvatarURL(user.getId()));
        builder.setUsername(user.getName());
    }

    public void setServerUserMeta(@NotNull final WebhookMessageBuilder builder) {
        builder.setUsername(DISCORD_FAB.getConfig().serverUserName);
        builder.setAvatarUrl(DISCORD_FAB.getConfig().serverUserAvatarUrl);
    }

    public void shutdown() {
        this.onServerEvent(ServerEvent.STOP);
        this.webhookClientHolder.closeAll();
    }

    public void onServerEvent(@NotNull final ServerEvent event) {
        String message;
        Color color;
        if (event == ServerEvent.START) {
            message = CONFIG.event.serverStart;
            color = Color.GREEN;
        } else {
            message = CONFIG.event.serverStop;
            color = Color.RED;
        }

        final EmbedBuilder builder = new EmbedBuilder().setTitle(message).setColor(color);
        this.broadcast(builder.build());
    }

    private enum MappedChannel {
        PUBLIC("public_chat", CONFIG.publicChat, ServerChat.Channel.PUBLIC),
        STAFF("staff_chat", CONFIG.staffChat, ServerChat.Channel.STAFF),
        BUILDER("builder_chat", CONFIG.builderChat, ServerChat.Channel.BUILDER),
        SOCIAL_SPY("social_spy", CONFIG.socialSpy, null, false);

        final String id;
        final ChatChannelSynchronizerConfigSection config;
        @Nullable
        final ServerChat.Channel channel;
        final boolean toMinecraft;

        MappedChannel(final String id, final ChatChannelSynchronizerConfigSection config, @Nullable final ServerChat.Channel channel) {
            this(id, config, channel, true);
        }

        MappedChannel(final String id, final ChatChannelSynchronizerConfigSection config, @Nullable final ServerChat.Channel channel, final boolean toMinecraft) {
            this.id = id;
            this.config = config;
            this.channel = channel;
            this.toMinecraft = toMinecraft;
        }

        @Nullable
        public static ChatSynchronizer.MappedChannel byServerChannel(@NotNull final ServerChat.Channel channel) {
            for (MappedChannel value : values()) {
                if (value.channel == null) {
                    continue;
                }

                if (value.channel.equals(channel)) {
                    return value;
                }
            }

            return null;
        }

        @Nullable
        public static ChatSynchronizer.MappedChannel byChannelId(final long id) {
            for (MappedChannel value : values()) {
                if (value.config.discordChannelId == id) {
                    return value;
                }
            }

            return null;
        }

        @Nullable
        public TextChannel getTextChannel() {
            return DISCORD_FAB.getGuild().getTextChannelById(this.config.discordChannelId);
        }

        public boolean isEnabled() {
            return this.config.enabled;
        }
    }

    public enum ServerEvent {
        START,
        STOP;
    }
}
