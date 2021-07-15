package com.github.hansi132.discordfab.discordbot.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

public class Constants {
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.##");
    public static final String WORKING_DIR = System.getProperty("user.dir");
    public static final Path WORKING_PATH = Paths.get(WORKING_DIR);
    public static final Path CONFIG_PATH = WORKING_PATH.resolve("DiscordFab");
    public static final String linkedAccountsDatabase = "CREATE TABLE IF NOT EXISTS linkedaccounts (\n"
            + "    LinkKey INTEGER NOT NULL,\n"
            + "    McUUID VARCHAR(255) NOT NULL,\n"
            + "    DiscordId BIGINT(20),\n"
            + "    McUsername VARCHAR(255) NOT NULL,\n"
            + "    PRIMARY KEY (LinkKey)\n"
            + ");";
    public static final String trackedinvitesDatabase = "CREATE TABLE IF NOT EXISTS trackedinvites (\n"
            + "    Id INTEGER NOT NULL AUTO_INCREMENT,\n"
            + "    InviterDiscordId BIGINT(20),\n"
            + "    InvitedDiscordId BIGINT(20),\n"
            + "    PRIMARY KEY (Id)\n"
            + ");";

    public static final String trackedSuggestions = "CREATE TABLE IF NOT EXISTS trackedsuggestions (\n"
            + "    Id INTEGER NOT NULL AUTO_INCREMENT,\n"
            + "    SuggestionMessageId BIGINT(20),\n"
            + "    StaffMessageId BIGINT(20),\n"
            + "    PRIMARY KEY (Id)\n"
            + ");";
}
