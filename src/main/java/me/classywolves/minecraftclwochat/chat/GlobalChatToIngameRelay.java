package me.classywolves.minecraftclwochat.chat;

import me.classywolves.minecraftclwochat.util.CLWOChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;

public class GlobalChatToIngameRelay {

    private List<GlobalChatMessage> messageList;
    private final FileConfiguration settingsConfig;

    public GlobalChatToIngameRelay(FileConfiguration settingsConfig) {
        this.settingsConfig = settingsConfig;
    }

    private void flushMessages() {

        // does this look messy ?
        messageList.forEach(message -> {
            String chatMessage = CLWOChatUtil.cc(message.getMessage());
            Bukkit.getServer().getConsoleSender().sendMessage(chatMessage);
            Bukkit.getWorlds().forEach(world -> {
                world.getPlayers().forEach(player -> {
                    player.sendMessage(chatMessage);
                });
            });
        });

        messageList = null;

    }

    public void relayChatMessages(List<GlobalChatMessage> messages) {
        messageList = messages;
        flushMessages();
    }

    public void relayChatMessage(GlobalChatMessage message) {
        List<GlobalChatMessage> messageAsList = new ArrayList<>();
        messageAsList.add(message);
        messageList = messageAsList;
        flushMessages();
    }






}
