package com.github.hansi132.discordfab.discordbot.command;

import com.github.hansi132.discordfab.discordbot.util.DatabaseConnection;
import com.github.hansi132.discordfab.discordbot.util.LinkKeyCreator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.kilocraft.essentials.api.KiloServer;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.util.messages.nodes.ExceptionMessageNode;
import org.kilocraft.essentials.util.text.Texter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class DiscordLinkCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("discord_link")
                .executes(DiscordLinkCommand::execute);

        dispatcher.register(builder);
        dispatcher.register(CommandManager.literal("link").executes(DiscordLinkCommand::execute));
    }

    private static int execute(final CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        OnlineUser user = KiloServer.getServer().getOnlineUser(ctx.getSource().getPlayer());
        Random random = new Random();
        int testKey = random.nextInt(10000);
        int linkKey;

        try {
            Connection connection = DatabaseConnection.getConnection();
            linkKey = LinkKeyCreator.checkKey(testKey);

            String selectSql = "SELECT McUUID, DiscordId, LinkKey FROM linkedaccounts WHERE McUUID = ?;";
            PreparedStatement selectStatement = connection.prepareStatement(selectSql);
            selectStatement.setString(1, user.getUuid().toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                user.sendMessage(
                        getText(Texter.newText("You are already linked!&r Your link is: "),
                                resultSet.getInt("LinkKey"))
                );

                return -1;
            }

            user.sendMessage(getText(Texter.newText("Your link key is: "), linkKey));

            String insertSql = "INSERT INTO linkedaccounts (LinkKey, McUUID, McUsername) VALUES (?,?,?);";
            PreparedStatement insertStatement = connection.prepareStatement(insertSql);
            insertStatement.setInt(1, linkKey);
            insertStatement.setString(2, user.getUuid().toString());
            insertStatement.setString(3, user.getUsername());
            insertStatement.execute();

        } catch (SQLException e) {
            user.sendMessage(
                    Texter.newText("Unexpected database error.").styled((style) ->
                            style.withHoverEvent(Texter.Events.onHover(e.getMessage())))
            );
        } catch (ClassNotFoundException e) {
            user.sendError(ExceptionMessageNode.UNKNOWN_COMMAND_EXCEPTION);
        }

        return 1;
    }

    private static MutableText getText(MutableText text, final int linkKey) {
        String key = String.format("%04d",linkKey);
        return Texter.newText().append(
                text.append(
                        Texter.newText(key).formatted(Formatting.AQUA).styled((style) ->
                                style.withHoverEvent(Texter.Events.onHover("Click to Copy"))
                                        .withClickEvent(
                                                new ClickEvent(
                                                        ClickEvent.Action.COPY_TO_CLIPBOARD,
                                                        key
                                                )
                                        )
                        )
                )
        );
    }
}
