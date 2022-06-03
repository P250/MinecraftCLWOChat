package me.classywolves.minecraftclwochat.runnables;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.classywolves.minecraftclwochat.chat.GlobalChatMessage;
import me.classywolves.minecraftclwochat.chat.GlobalChatToIngameRelay;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CLWOGlobalChatReaderRunnable extends BukkitRunnable {

    private HttpURLConnection api;
    private final GlobalChatToIngameRelay globalRelay;
    private int currentMessageID;
    private int oldMessageID;

    private void createNewConnection() {
        try {
            URL apiUrl = new URL(
                    "https://trclwo.inilo.net/mc/chat/98eb470b2b60482e259d28648895d9e1.php?i_want_to=read&last_chat_was="
                            + (currentMessageID)
            );

            api = (HttpURLConnection) apiUrl.openConnection();
            api.setRequestMethod("GET");
            api.setRequestProperty("User-Agent", "CLWO/1.0");
            api.setConnectTimeout(8000);

            api.connect();

        } catch (SocketTimeoutException socketEx) {
            Bukkit.getLogger().info("API connection timed out... Restarting connection.");
            createNewConnection();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    public CLWOGlobalChatReaderRunnable(int baseMessageID, GlobalChatToIngameRelay globalRelay) {
        currentMessageID = baseMessageID;
        this.globalRelay = globalRelay;
    }

    @Override
    public void run() {
        try {

            createNewConnection();

            /**
             * Response code 200/202 indicates new messages, we need to parse and store these messages
             */
            if (api.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(api.getInputStream()));

                String jsonString = reader.readLine();
                JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
                JsonArray data = json.getAsJsonArray("data");

                /**
                 * If the data array is bigger than 1 it means there are multiple messages to parse
                 * In addition we need to get the highest messageID and store it so we know if there are new ones.
                 */
                if (data.size() != 1) {
                    List<GlobalChatMessage> messages = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        JsonObject jsonMessage = data.get(i).getAsJsonObject();
                        if (i == data.size() - 1) {
                            oldMessageID = jsonMessage.get("chat_id").getAsInt();
                        }
                        GlobalChatMessage clwoMessage = new GlobalChatMessage(
                                jsonMessage.get("message").getAsString()
                        );
                        messages.add(clwoMessage);
                    }
                    globalRelay.relayChatMessages(messages);
                } else {
                    JsonObject jsonMessage = data.get(0).getAsJsonObject();
                    oldMessageID = jsonMessage.get("chat_id").getAsInt();
                    GlobalChatMessage clwoMessage = new GlobalChatMessage(
                            jsonMessage.get("message").getAsString()
                    );
                    globalRelay.relayChatMessage(clwoMessage);
                }

                currentMessageID = oldMessageID;
                api.disconnect();

            }
        } catch (IOException ex) {
            if (ex.getCause().equals(new SocketTimeoutException())) {
                System.out.println("[MinecraftCLWOChat] Client was disconnected from api. . . Starting new connection.");
                createNewConnection();
            } else {
                System.out.println("[MinecraftCLWOChat] There was a critical error with receiving new messages.");
                ex.printStackTrace();
                cancel();
            }
        }
    }
}
