package com.github.hansi132.discordfab.discordbot.util;

import com.github.hansi132.discordfab.DiscordFab;
import com.github.hansi132.discordfab.discordbot.config.DataConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final DataConfig config = DiscordFab.getInstance().getDataConfig();
    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection != null) return connection;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    config.getProperty("database"),
                    config.getProperty("databaseUser"),
                    config.getProperty("databasePassword"));
            return connection;
        } catch (SQLException | ClassNotFoundException throwables) {
            throw new RuntimeException("An error occurred trying to connect to the database: ", throwables);
        }
    }

    public static void close() throws SQLException {
        connection.close();
        connection = null;
    }

}
