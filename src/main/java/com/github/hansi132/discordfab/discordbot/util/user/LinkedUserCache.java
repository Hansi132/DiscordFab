package com.github.hansi132.discordfab.discordbot.util.user;

import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LinkedUserCache extends ArrayList<LinkedUser> {

    public LinkedUserCache() {
        Connection conn = DatabaseConnection.getConnection();
        String selectSql = "SELECT * FROM linkedaccounts;";
        try {
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            List<LinkedUser> linkedUsers = getUsers(selectStmt.executeQuery());
            this.addAll(linkedUsers);
        } catch (SQLException throwables) {
            DiscordFab.LOGGER.error("There was an error while initializing user cache: ", throwables);
        }
    }

    public Optional<LinkedUser> getByKey(int linkKey) {
        try {
            for (LinkedUser linkedUser : this) {
                if (linkedUser.getLinkKey() == linkKey) return Optional.of(linkedUser);
            }
            Connection conn = DatabaseConnection.getConnection();
            String selectSql = "SELECT * FROM linkedaccounts WHERE LinkKey = ?;";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setInt(1, linkKey);
            List<LinkedUser> linkedUsers = getUsers(selectStmt.executeQuery());
            return linkedUsers.isEmpty() ? Optional.empty() : Optional.ofNullable(linkedUsers.get(0));
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
            return Optional.empty();
        }
    }

    public Optional<LinkedUser> getByUUID(UUID mcUUID) {
        try {
            for (LinkedUser linkedUser : this) {
                if (linkedUser.getMcUUID() == mcUUID) return Optional.of(linkedUser);
            }
            Connection conn = DatabaseConnection.getConnection();
            String selectSql = "SELECT * FROM linkedaccounts WHERE McUUID = ?;";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setString(1, mcUUID.toString());
            List<LinkedUser> linkedUsers = getUsers(selectStmt.executeQuery());
            return linkedUsers.isEmpty() ? Optional.empty() : Optional.ofNullable(linkedUsers.get(0));
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
            return Optional.empty();
        }
    }

    public Optional<LinkedUser> getByDiscordID(long discordID) {
        try {
            for (LinkedUser linkedUser : this) {
                if (linkedUser.getDiscordID().isPresent() && linkedUser.getDiscordID().get() == discordID)
                    return Optional.of(linkedUser);
            }
            Connection conn = DatabaseConnection.getConnection();
            String selectSql = "SELECT * FROM linkedaccounts WHERE DiscordId = ?;";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setLong(1, discordID);
            List<LinkedUser> linkedUsers = getUsers(selectStmt.executeQuery());
            return linkedUsers.isEmpty() ? Optional.empty() : Optional.ofNullable(linkedUsers.get(0));
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
            return Optional.empty();
        }

    }

    private List<LinkedUser> getUsers(ResultSet resultSet) throws SQLException {
        List<LinkedUser> list = new ArrayList<>();
        while (resultSet.next()) {
            final int linkKey = resultSet.getInt("LinkKey");
            final long discordId = resultSet.getLong("DiscordId");
            final UUID mcUUID = UUID.fromString(resultSet.getString("McUUID"));
            final String mcName = resultSet.getString("McUsername");
            LinkedUser linkedUser = discordId == 0L ? new LinkedUser(linkKey, mcUUID, mcName) : new LinkedUser(linkKey, mcUUID, discordId, mcName);
            list.add(linkedUser);
        }
        return list;
    }

    public void removeUser(LinkedUser linkedUser) {
        this.remove(linkedUser);
        try {
            Connection conn = DatabaseConnection.getConnection();
            String deleteSql = "DELETE FROM linkedaccounts WHERE LinkKey = ?;";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, linkedUser.getLinkKey());
            deleteStmt.execute();
        } catch (SQLException e) {
            DiscordFab.LOGGER.error("Could not query the database!", e);
        }
    }

}
