package me.classywolves.minecraftclwochat.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.classywolves.minecraftclwochat.MinecraftCLWOChat;
import me.classywolves.minecraftclwochat.chat.GlobalChatToIngameRelay;
import me.classywolves.minecraftclwochat.util.CLWOChatUtil;
import me.classywolves.minecraftclwochat.util.SettingsConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.io.*;
import java.net.*;
import java.util.UUID;

public class PlayerSendGlobalMessageEvent implements Listener {

    private final FileConfiguration settingsConfig;
    private GlobalChatToIngameRelay relay;

    /**
     *
     * @param method - API call method
     * @param uuid - UUID of player sending a message
     * @param message - Text the player wants to send
     * @return - Message to relay to player, indicating if it was successful or not
     *
     */
    private String sendPlayerMessage(String method, String uuid, String message) {
        try {
            URL apiUrl = new URL(
                    "https://trclwo.inilo.net/mc/chat/98eb470b2b60482e259d28648895d9e1.php"
                    + "?i_want_to=" + method
                    + "&my_uuid=" + uuid
                    + "&my_message=" + URLEncoder.encode(message, "UTF-8")
            );
            HttpURLConnection api = (HttpURLConnection) apiUrl.openConnection();

            api.setRequestMethod("GET");
            api.setRequestProperty("User-Agent", "CLWO/1.0");
            api.setConnectTimeout(8000);

            api.connect();

            if (api.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {
                return CLWOChatUtil.cc(
                        settingsConfig.getString(SettingsConfig.GLOBALCHAT_SEND_MESSAGE)
                );
            } else if (api.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(api.getErrorStream()));
                String jsonString = errorReader.readLine();
                JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();

                return "&c&o[CLWOChat] " + json.get("message").getAsString();

            } else {
                return "[CLWOChat] API returned some whacky response code.. Code: " + api.getResponseCode();
            }


        } catch (SocketTimeoutException ioEx) {
            Bukkit.getServer().getLogger().warning("API Connected timed out... Reconnecting.");
            sendPlayerMessage(method, uuid, message);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            return "&c&o[CLWOChat] You must wait before sending a message again.";
        }
        return "[CLWOChat] You shouldn't be seeing this ...";
    }

    public PlayerSendGlobalMessageEvent(MinecraftCLWOChat plugin) {
        this.settingsConfig = plugin.getConfig();
        relay = plugin.getGlobalChatToIngameRelay();
    }


    @EventHandler
    public void playerChatEvent(AsyncPlayerChatEvent event) {

        Player pl = event.getPlayer();
        UUID plUUID = pl.getUniqueId();
        String rawPlayerMessage = event.getMessage();

        if (!(rawPlayerMessage.startsWith("##"))) {
            return;
        }

        event.setCancelled(true);

        String actualPlayerMessage = rawPlayerMessage.substring(2);
        String response = sendPlayerMessage("write", plUUID.toString(), actualPlayerMessage);

        pl.sendMessage(CLWOChatUtil.cc(response));

    }

}
