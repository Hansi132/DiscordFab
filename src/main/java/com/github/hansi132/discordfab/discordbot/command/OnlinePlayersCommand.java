package com.github.hansi132.discordfab.discordbot.command;

import com.github.hansi132.discordfab.discordbot.api.command.BotCommandSource;
import com.github.hansi132.discordfab.discordbot.api.command.CommandCategory;
import com.github.hansi132.discordfab.discordbot.api.command.DiscordFabCommand;
import com.mojang.brigadier.context.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.user.preference.Preferences;

import java.util.ArrayList;
import java.util.List;


public class OnlinePlayersCommand extends DiscordFabCommand {

    public OnlinePlayersCommand(@NotNull CommandCategory category, @NotNull String label, @Nullable String... alias) {
        super(category, label, alias);
        this.withDescription("Gives you the current online players.");
        this.argBuilder.executes(this::execute);
    }

    private int execute(CommandContext<BotCommandSource> ctx) {
        BotCommandSource src = ctx.getSource();
        List<OnlineUser> users = KiloEssentials.getServer().getUserManager().getOnlineUsersAsList();
        List<OnlineUser> vanishedUsers = new ArrayList<>();

        for (OnlineUser user : users) {
            if (user.getPreference(Preferences.VANISH)) {
                vanishedUsers.add(user);
            }
        }

        users.removeAll(vanishedUsers);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(DISCORD_FAB.getEmbedUtil().getDefaultColor())
                .setTitle(users.size() + " Players Online");

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < users.size(); i++) {
            stringBuilder.append('`').append(users.get(i).getName()).append('`');

            if (i + 1 < users.size()) {
                stringBuilder.append(' ');
            }
        }

        builder.addField("", stringBuilder.toString(), false);
        src.sendFeedback(builder.build()).queue();
        return SUCCESS;
    }
}

