package com.infectedbytes.discordbot;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.ServerChatEvent;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = DiscordBot.MOD_ID, version = DiscordBot.VERSION, acceptableRemoteVersions = "*")
public class DiscordBot {
    static final String MOD_ID = "discordbot";
    static final String VERSION = "1.0";

    private Configuration config;
    private JDA jda;
    private TextChannel chatChannel;
    private WebhookClient client;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        config = new Configuration(e.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        setupDiscord();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MinecraftForwarder());
        FMLCommonHandler.instance().bus().register(new LoginHandler());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        shutdownDiscord();
    }

    private void setupDiscord(){
        try {
            config.load();
            Settings.read(config);
            jda = new JDABuilder(AccountType.BOT).setToken(Settings.token).buildBlocking();
            jda.addEventListener(new DiscordForwarder());
            chatChannel = jda.getTextChannelById(Settings.channelId);
            client = new WebhookClientBuilder(Settings.webhook).build();
            updatePlayerList(null);
        } catch (Exception ex) {
            ex.printStackTrace();
            shutdownDiscord();
        } finally {
            if (config.hasChanged()) config.save();
        }
    }

    private void shutdownDiscord() {
        if(client != null) {
            client.close();
            client = null;
        }
        if(jda != null) {
            jda.shutdownNow();
            jda = null;
            chatChannel = null;
        }
    }

    private void updatePlayerList(List<EntityPlayerMP> players) {
        if (chatChannel == null) return;
        String title = Settings.Messages.getUsers(players);
        try {
            chatChannel.getManager().setTopic(title).queue();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendGlobal(String msg) {
        for (Object entity : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            ((EntityPlayerMP) entity).addChatMessage(new ChatComponentText(msg));
        }
    }

    @SuppressWarnings("unchecked")
    public class LoginHandler {
        @SubscribeEvent
        public void login(PlayerEvent.PlayerLoggedInEvent e) {
            updatePlayerList((List<EntityPlayerMP>) MinecraftServer.getServer().getConfigurationManager().playerEntityList);
        }

        @SubscribeEvent
        public void logout(PlayerEvent.PlayerLoggedOutEvent e) {
            List<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>(MinecraftServer.getServer().getConfigurationManager().playerEntityList);
            //noinspection SuspiciousMethodCalls
            players.remove(e.player);
            updatePlayerList(players);
        }
    }

    public class MinecraftForwarder {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onChatEvent(ServerChatEvent e) {
            if (chatChannel == null || e.player == null || client == null) return;
            try {
                client.send(new WebhookMessageBuilder()
                        .setAvatarUrl("https://crafatar.com/renders/head/" + e.player.getUniqueID().toString())
                        .setUsername(e.username)
                        .setContent(e.message).build());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class DiscordForwarder extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;
            if (event.getChannel().getIdLong() != Settings.channelId) return;
            String msg = Settings.Messages.formatFromDiscord(event.getAuthor().getName(), event.getMessage().getContentDisplay());
            if (msg != null) sendGlobal(msg);
        }
    }
}
