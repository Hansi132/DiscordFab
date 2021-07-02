package com.github.hansi132.discordfab.discordbot.integration;

import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.util.user.LinkedUser;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.query.QueryOptions;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.util.EntityIdentifiable;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserSynchronizer {
    private static final Logger LOGGER = DiscordFab.LOGGER;
    private static final DiscordFab DISCORD_FAB = DiscordFab.getInstance();
    private static final ShardManager BOT = DiscordFab.getBot();
    private static final Pattern LINK_KEY_PATTERN = Pattern.compile("(\\d{4})");

    public static boolean isLinkCode(@NotNull final String string) {
        Matcher matcher = LINK_KEY_PATTERN.matcher(string);
        return matcher.find();
    }

    public static int getLinkCode(@NotNull final String string) {
        Matcher matcher = LINK_KEY_PATTERN.matcher(string);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public static void sync(final PrivateChannel privateChannel, MessageChannel publicChannel, @NotNull final User user, final int linkKey) {
        Optional<LinkedUser> optional = DiscordFab.getInstance().getUserCache().getByKey(linkKey);
        if (optional.isPresent()) {
            LinkedUser linkedUser = optional.get();
            UUID mcUUID = linkedUser.getMcUUID();
            linkedUser.updateDiscordID(user.getIdLong());
            KiloEssentials.getInstance().getUserThenAcceptAsync(mcUUID, (optionalUser) -> {
                if (optionalUser.isPresent()) {
                    org.kilocraft.essentials.api.user.User playerUser = optionalUser.get();
                    if (privateChannel != null) {
                        privateChannel.sendMessage(
                                DISCORD_FAB.getConfig().messages.successfully_linked
                                        .replace("%player%", playerUser.getName())
                        ).queue();
                    } else if (publicChannel != null) {
                        publicChannel.sendMessage(
                                DISCORD_FAB.getConfig().messages.successfully_linked
                                        .replace("%player%", playerUser.getName())
                        ).queue();
                    }

                    KiloEssentials.getServer().execute(DISCORD_FAB.getConfig().userSync.command
                            .replace("%player%", playerUser.getName()));

                    if (DISCORD_FAB.getConfig().userSync.syncDisplayName) {
                        try {
                            syncDisplayName(linkedUser);
                        } catch (SQLException | ClassNotFoundException e) {
                            LOGGER.error("Unexpected error while trying to sync user", e);
                        }
                    }
                    syncRoles(user.getIdLong(), linkedUser.getMcUUID());
                }
            });
        } else {
            if (privateChannel != null) {
                privateChannel.sendMessage(DISCORD_FAB.getConfig().messages.invalid_link_key).queue();
            } else if (publicChannel != null) {
                publicChannel.sendMessage(DISCORD_FAB.getConfig().messages.invalid_link_key).queue();
            }
        }
    }

    public static void clearMuteRole() throws SQLException {
        Guild guild = DISCORD_FAB.getGuild();
        Role role = guild.getRoleById(DISCORD_FAB.getConfig().userSync.mutedRoleId);
        if (role != null) {
            for (Member member : guild.getMembersWithRoles(role)) {
                Optional<LinkedUser> optional = DiscordFab.getInstance().getUserCache().getByDiscordID(member.getIdLong());
                if (optional.isPresent()) {
                    LinkedUser linkedUser = optional.get();
                    boolean isMuted = KiloEssentials.getServer().getUserManager().getPunishmentManager().isMuted(new EntityIdentifiable() {
                        public UUID getId() {
                            return linkedUser.getMcUUID();
                        }

                        public String getName() {
                            return linkedUser.getMcName();
                        }
                    });
                    if (!isMuted) {
                        guild.removeRoleFromMember(member, role).queue();
                    }
                }
            }
        }
    }

    public static void syncRoles(final UUID mcUUID) throws SQLException, ClassNotFoundException {
        DiscordFab.getInstance().getUserCache().getByUUID(mcUUID).ifPresent(linkedUser -> linkedUser.getDiscordID().ifPresent(discordID -> syncRoles(discordID, linkedUser.getMcUUID())));
    }

    private static void syncRoles(final long discordID, final UUID mcUUID) {
        Guild guild = DISCORD_FAB.getGuild();
        int highestRole = 0;
        for (Role r : guild.getSelfMember().getRoles()) {
            highestRole = Math.max(highestRole, r.getPosition());
        }
        for (Role role : guild.getRoles()) {
            if (role.getName().equals("@everyone") || role.getPosition() >= highestRole) continue;
            long roleID = role.getIdLong();
            if (!DISCORD_FAB.getConfig().userSync.syncedRoles.contains(roleID)) continue;
            if (shouldSync(mcUUID, roleID)) {
                User user = BOT.getUserById(discordID);
                if (user != null) {
                    Member member = guild.getMember(user);
                    if (member != null) {
                        try {
                            guild.addRoleToMember(member, role).queue();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    private static boolean shouldSync(UUID uuid, long roleID) {
        final String SYNC_PERM_PREFIX = "discordfab.sync.";
        LuckPerms luckPerms = LuckPermsProvider.get();
        net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            QueryOptions options = luckPerms.getContextManager().getStaticQueryOptions();
            return user.getCachedData().getPermissionData(options).checkPermission(SYNC_PERM_PREFIX + roleID).asBoolean();
        }
        return false;
    }

    private static void syncDisplayName(final LinkedUser linkedUser) throws SQLException, ClassNotFoundException {
        Guild guild = DISCORD_FAB.getGuild();
        if (!linkedUser.getDiscordID().isPresent()) return;
        User user = BOT.getUserById(linkedUser.getDiscordID().get());
        Role role = guild.getRoleById(DISCORD_FAB.getConfig().userSync.linkedRoleId);

        if (role == null) {
            LOGGER.warn("There is no Linked Role Selected!");
        } else if (user != null) {
            Member member = guild.getMember(user);
            if (member != null) {
                try {
                    member.modifyNickname(linkedUser.getMcName()).complete();
                } catch (HierarchyException | NullPointerException ignored) {
                }
                guild.addRoleToMember(member, role).queue();
            }

        }

    }
}
