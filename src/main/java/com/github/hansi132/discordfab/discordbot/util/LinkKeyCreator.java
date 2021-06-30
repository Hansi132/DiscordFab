package com.github.hansi132.discordfab.discordbot.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class LinkKeyCreator {
    public static int checkKey(int linkKey) throws SQLException, ClassNotFoundException {
        return checkKey(linkKey, DatabaseConnection.getConnection());
    }

    private static int checkKey(int linkKey, Connection conn) throws SQLException {
        Random random = new Random();

        String selectSql = "SELECT LinkKey FROM linkedaccounts WHERE LinkKey = ?;";
        PreparedStatement selectStatement = conn.prepareStatement(selectSql);
        selectStatement.setInt(1, linkKey);
        ResultSet checkDuplicateRs = selectStatement.executeQuery();

        if (checkDuplicateRs.next()) {
            linkKey = random.nextInt(10000);
            return checkKey(linkKey, conn);
        }

        return linkKey;
    }
}
