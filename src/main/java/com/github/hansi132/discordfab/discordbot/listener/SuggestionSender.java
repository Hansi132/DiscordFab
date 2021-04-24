package com.github.hansi132.discordfab.discordbot.listener;

import com.github.hansi132.discordfab.DiscordFab;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.util.Objects;

public class SuggestionSender {

    public SuggestionSender(GuildMessageReceivedEvent event) {
        String raw = event.getMessage().getContentRaw();
        Member member = event.getMember();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.green);
        embedBuilder.setTitle("Suggestion");
        embedBuilder.setThumbnail(member != null ? member.getUser().getAvatarUrl() : "https://cdn.discordapp.com/avatars/722172693083979817/ef6bbb5b3145dcced312342eb1c3ea47.png?size=128");
        embedBuilder.addField((member != null ? member.getEffectiveName() : event.getAuthor().getName()) + " suggested:", raw, false);
        embedBuilder.setFooter("Please upvote or downvote.");
        event.getMessage().delete().queue();
        event.getChannel().sendMessage(embedBuilder.build()).queue(message -> {
            message.addReaction(":upvote:657228604312256521").queue();
            message.addReaction(":downvote:657228570338394142").queue();
        });
        Objects.requireNonNull(DiscordFab.getBot().getTextChannelById(DiscordFab.getInstance().getConfig().suggestionAdminChatId)).sendMessage(embedBuilder.build()).queue();
    }
}
