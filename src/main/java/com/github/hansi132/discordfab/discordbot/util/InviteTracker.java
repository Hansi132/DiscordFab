package com.github.hansi132.discordfab.discordbot.util;

import com.github.hansi132.discordfab.DiscordFab;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.invite.GenericGuildInviteEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class InviteTracker {
    HashMap<Invite, Integer> inviteCache = new HashMap<>();

    public void cacheInvites(Guild guild) {
        inviteCache.clear();
        guild.retrieveInvites().queue(invites -> {
            for (Invite invite : invites) {
                inviteCache.put(invite, invite.getUses());
            }
        });
    }

    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        DiscordFab.LOGGER.info("onGuildMemberJoinEvent: " + event.getUser().getName() + " joined " + event.getGuild().getName());
        event.getGuild().retrieveInvites().queue(invites -> {
            for (Invite invite : invites) {
                User inviter = invite.getInviter();
                if (inviter == null) {
                    DiscordFab.LOGGER.error("Inviter is null");
                    continue;
                }
                if (!inviteCache.containsKey(invite)) {
                    DiscordFab.LOGGER.info(invite.getCode() + " by " + invite.getInviter().getName() + " hasn't been cached WTF");
                    continue;
                }
                int i = inviteCache.get(invite);
                DiscordFab.LOGGER.info("Checking " + invite.getCode() + " by " + invite.getInviter().getName() + ", cached uses: " + i + ", uses: " + invite.getUses());
                if (invite.getUses() != i) {
                    long inviterID = inviter.getIdLong();
                    long invitedID = event.getUser().getIdLong();
                    DiscordFab.LOGGER.info(inviter.getName() + " invited " + event.getUser().getName() + " to " + event.getGuild().getName());
                    this.addEntry(inviterID, invitedID);
                    this.cacheInvites(event.getGuild());
                    return;
                } else {
                    DiscordFab.LOGGER.info(invite.getCode() + " didn't change and isn't eligible");
                }
            }
            DiscordFab.LOGGER.error("This shouldn't have happened! A new member joined, but we couldn't detect who invited them!");
            this.cacheInvites(event.getGuild());
        });
    }

    public void onGuildInviteChange(@Nonnull GenericGuildInviteEvent event) {
        DiscordFab.LOGGER.info(event.getCode() + " changed " + "(" + (event instanceof GuildInviteDeleteEvent ? "deleted" : "created") + ")" + " caching.");
        this.cacheInvites(event.getGuild());
    }

    public int getTotalInvites(long inviter) {
        return getInvitedIDs(inviter).size();
    }

    public int getValidInvites(Guild guild, long inviter) {
        int size = 0;
        for (long invited : getInvitedIDs(inviter)) {
            if (guild.getMemberById(invited) != null) size++;
        }
        return size;
    }

    public int getLinkedInvites(Guild guild, long inviter) {
        int size = 0;
        for (long invited : getInvitedIDs(inviter)) {
            if (DiscordFab.getInstance().getUserCache().getByDiscordID(invited).isPresent() && guild.getMemberById(invited) != null) size++;
        }
        return size;
    }

    private List<Long> getInvitedIDs(long inviter) {
        List<Long> list = new ArrayList<>();
        LinkedHashSet<Long> hashSet = new LinkedHashSet<>();
        try {
            Connection connection = DatabaseConnection.getConnection();
            String selectSql = "SELECT * FROM trackedinvites WHERE InviterDiscordId = ?;";
            PreparedStatement insertStatement = connection.prepareStatement(selectSql);
            insertStatement.setLong(1, inviter);
            ResultSet rs = insertStatement.executeQuery();
            while (rs.next()) {
                long invitedID = rs.getLong("InvitedDiscordId");
                //Prevent duplicates
                if (hashSet.add(invitedID)) list.add(invitedID);
            }
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
        }
        return list;
    }

    public List<Long> getInviterIDs() {
        List<Long> list = new ArrayList<>();
        LinkedHashSet<Long> hashSet = new LinkedHashSet<>();
        try {
            Connection connection = DatabaseConnection.getConnection();
            String selectSql = "SELECT * FROM trackedinvites;";
            PreparedStatement insertStatement = connection.prepareStatement(selectSql);
            ResultSet rs = insertStatement.executeQuery();
            while (rs.next()) {
                long inviterID = rs.getLong("InviterDiscordId");
                //Prevent duplicates
                if (hashSet.add(inviterID)) list.add(inviterID);
            }
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
        }
        return list;
    }

    private void addEntry(long inviter, long invited) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            String insertSql = "INSERT INTO trackedinvites (InviterDiscordId, InvitedDiscordId) VALUES (?, ?);";
            PreparedStatement insertStatement = connection.prepareStatement(insertSql);
            insertStatement.setLong(1, inviter);
            insertStatement.setLong(2, invited);
            insertStatement.execute();
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
        }
    }
}
