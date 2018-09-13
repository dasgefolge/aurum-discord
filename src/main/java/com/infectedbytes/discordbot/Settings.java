package com.infectedbytes.discordbot;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.config.Configuration;

import java.util.List;

final class Settings {
    static String token;
    static long channelId;
    static String webhook;

    static class Messages {
        static String empty;
        static String many;
        static String normal;
        static String fromDiscord;

        static String getUsers(List<EntityPlayerMP> users) {
            if (users == null || users.size() == 0)
                return empty;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < users.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(users.get(i).getDisplayName());
            }

            try {
                return sb.length() > 1024
                        ? String.format(many, users.size())
                        : String.format(normal, sb.toString());
            } catch (Exception ex) {
                return "Error";
            }
        }

        static String formatFromDiscord(String user, String msg) {
            try {
                return String.format(fromDiscord, user, msg);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    static void read(Configuration config){
        token = config.get("general", "token", "").getString();
        webhook = config.get("general", "webhook", "").getString();
        channelId = Long.parseLong(config.get("general", "channelId", "0").getString());

        Messages.empty = config.get("messages", "empty", "Server is abandoned").getString();
        Messages.many = config.get("messages", "many", "%d users are currently online").getString();
        Messages.normal = config.get("messages", "normal", "Online: %s").getString();

        Messages.fromDiscord = config.get("messages", "fromDiscord", "[§2Discord§r] %s: %s").getString();
    }
}
