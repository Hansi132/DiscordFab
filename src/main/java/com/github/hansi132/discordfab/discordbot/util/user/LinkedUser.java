package com.github.hansi132.discordfab.discordbot.util.user;

import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class LinkedUser {

    private final int linkKey;
    private final UUID mcUUID;
    private Optional<Long> discordID;
    private String mcName;


    public LinkedUser(int linkKey, UUID mcUUID, long discordID, String mcName) {
        this.linkKey = linkKey;
        this.mcUUID = mcUUID;
        this.discordID = Optional.of(discordID);
        this.mcName = mcName;
    }

    public LinkedUser(int linkKey, UUID mcUUID, String mcName) {
        this.linkKey = linkKey;
        this.mcUUID = mcUUID;
        this.discordID = Optional.empty();
        this.mcName = mcName;
    }

    public int getLinkKey() {
        return linkKey;
    }

    public UUID getMcUUID() {
        return mcUUID;
    }

    public Optional<Long> getDiscordID() {
        return discordID;
    }

    public void updateName(String mcName) {
        if (!this.mcName.equals(mcName)) {
            Connection conn = DatabaseConnection.getConnection();
            String updateSql = "UPDATE linkedaccounts SET McUsername = ? WHERE LinkKey = ?;";
            try {
                PreparedStatement updateStatement = conn.prepareStatement(updateSql);
                updateStatement.setString(1, mcName);
                updateStatement.setInt(2, this.getLinkKey());
                updateStatement.execute();
                this.mcName = mcName;
            } catch (SQLException ex) {
                DiscordFab.LOGGER.error("Couldn't update name for " + this.toString(), ex);
            }
        }
    }

    public void updateDiscordID(long discordID) {
        Connection conn = DatabaseConnection.getConnection();
        String updateSql = "UPDATE linkedaccounts SET DiscordId = ? WHERE LinkKey = ?;";
        try {
            PreparedStatement updateStatement = conn.prepareStatement(updateSql);
            updateStatement.setLong(1, discordID);
            updateStatement.setInt(2, this.getLinkKey());
            updateStatement.execute();
            this.discordID = Optional.of(discordID);
        } catch (SQLException ex) {
            DiscordFab.LOGGER.error("Couldn't update discord id for " + this.toString(), ex);
        }
    }

    public String getMcName() {
        return mcName;
    }

    public boolean isComplete() {
        return discordID.isPresent();
    }

    public String toString() {
        return "LinkedUser[linkKey=" + linkKey + ", mcUUID=" + mcUUID + ", discordID=" + discordID + ", mcName=" + mcName + "]";
    }
}
