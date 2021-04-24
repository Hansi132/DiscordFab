package com.github.hansi132.discordfab;

import com.github.hansi132.discordfab.discordbot.ChatSynchronizer;
import com.github.hansi132.discordfab.discordbot.command.DiscordLinkCommand;
import com.github.hansi132.discordfab.discordbot.config.DataConfig;
import com.github.hansi132.discordfab.discordbot.integration.*;
import com.github.hansi132.discordfab.discordbot.listener.BanListener;
import com.github.hansi132.discordfab.discordbot.listener.ChatMessageListener;
import com.github.hansi132.discordfab.discordbot.listener.MuteListener;
import com.github.hansi132.discordfab.discordbot.listener.TickListener;
import com.github.hansi132.discordfab.discordbot.util.Constants;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.kilocraft.essentials.api.KiloServer;

import java.io.File;
import java.util.HashMap;

public class DiscordFabMod implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        final File CONFIG_FILE = Constants.CONFIG_PATH.toFile();
        if (!CONFIG_FILE.exists()) {
            CONFIG_FILE.mkdirs();
        }

        DataConfig config = new DataConfig(
                new HashMap<String, Object>() {{
                    this.put("token", "*paste bot token here*");
                    this.put("broadcastEnable", "*True or false*");
                    this.put("broadcastChannel", "*Paste what channel to broadcast to here*");
                    this.put("discordBroadcaster", "*Webhook url to cast Minecraft chat to*");
                    this.put("databaseUser", "*Specify the user of the database*");
                    this.put("databasePassword", "*Specify the password of the database*");
                    this.put("database", "*Specify the database to be used*");
                    this.put("guild", "*Specify the guild id*");
                }}
        );

        DiscordFab fab = new DiscordFab(config);

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> DiscordLinkCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            KiloServer.getServer().registerEvent(new ChatMessageListener());
            KiloServer.getServer().registerEvent(new BanListener());
            KiloServer.getServer().registerEvent(new MuteListener());
            KiloServer.getServer().registerEvent(new TickListener());
            KiloServer.getServer().registerEvent(new PlayerJoinBroadcaster());
            KiloServer.getServer().registerEvent(new PlayerLeaveBroadcaster());

            fab.getChatSynchronizer().onServerEvent(ChatSynchronizer.ServerEvent.START);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> fab.shutdown());
    }
}
