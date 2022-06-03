package me.classywolves.minecraftclwochat.runnables;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.classywolves.minecraftclwochat.MinecraftCLWOChat;
import me.classywolves.minecraftclwochat.chat.GlobalChatToIngameRelay;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class initGlobalChatReaderRunnable extends BukkitRunnable {

    private final MinecraftCLWOChat plugin;
    private final GlobalChatToIngameRelay globalRelay;
    private int baseMessageID;

    private void initGlobalChatReader() {
        try {
            // When the plugin loads we need to get a baseMessageID (the latest message) and keep reading on top of that
            URL url = new URL("https://trclwo.inilo.net/mc/chat/98eb470b2b60482e259d28648895d9e1.php?i_want_to=get_last_chat_id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "CLWO/1.0");

            conn.connect();

            if (!(conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                System.err.println("Something is wrong with the API endpoint. Stopping plugin . . .");
                Bukkit.getPluginManager().disablePlugin(plugin);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String jsonString = reader.readLine();
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            baseMessageID = json.get("last_chat_was").getAsInt();

            conn.disconnect();

            if (baseMessageID == -1) {
                System.err.println("Could not retrieve baseMessageID. Stopping plugin . . .");
                Bukkit.getPluginManager().disablePlugin(plugin);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Could not start global chat reading service. Stopping plugin . . .");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
        new CLWOGlobalChatReaderRunnable(baseMessageID, globalRelay).runTaskTimerAsynchronously(plugin, 0L, 200L);
    }

    public initGlobalChatReaderRunnable(MinecraftCLWOChat plugin, int baseMessageID, GlobalChatToIngameRelay relay) {
        this.plugin = plugin;
        this.baseMessageID = baseMessageID;
        this.globalRelay = relay;
    }

    @Override
    public void run() {
        initGlobalChatReader();
    }
}
