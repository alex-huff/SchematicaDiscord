package phonis.SchematicaDownload;

import org.bukkit.plugin.java.JavaPlugin;
import phonis.SchematicaDownload.commands.CommandPSchem;
import phonis.SchematicaDownload.discord.DiscordManager;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class SchematicaDownload extends JavaPlugin {

    public static final String path = "plugins/SchematicaDownload/";
    public static final String tokenPath = SchematicaDownload.path + "token.txt";
    public static final String linkPath = SchematicaDownload.path + "link.txt";
    public static String token = null;

    private Logger log;
    public DiscordManager dm;

    @Override
    public void onEnable() {
        this.log = getLogger();
        File f = new File(SchematicaDownload.path);

        if (!f.exists()) {
            if (f.mkdirs()) {
                log.info("Creating directory: " + SchematicaDownload.path + ".");
                log.info("Creating token file.");

                try {
                    this.createDefaultTokenFile();
                } catch (IOException e) {
                    log.warning("Could not create token file.");
                }

                log.info("Creating link file.");

                try {
                    this.createDefaultLinkFile();
                } catch (IOException e) {
                    log.warning("Could not create link file.");
                }
            }
        } else {
            try {
                this.loadToken();
            } catch (IOException e) {
                this.log.info("IOException while loading token.");
            }

            try {
                this.loadLinks();
            } catch (IOException e) {
                this.log.info("IOException while loading links.");
            }
        }

        if (SchematicaDownload.token != null) {
            this.dm = new DiscordManager(SchematicaDownload.token, this);
        } else {
            this.log.info("Token not specified, not starting Discord bot.");
        }

        new CommandPSchem(this);

        this.log.info("SchematicaDownload enable finished.");
    }

    @Override
    public void onDisable() {
        if (this.dm != null && this.dm.jda != null) {
            this.dm.jda.shutdownNow();
        }

        try {
            this.saveLinks();
        } catch (IOException e) {
            this.log.severe("Failed to save links.");
        }

        this.log.info("SchematicaDownload disable finished.");
    }

    private void createDefaultTokenFile() throws IOException {
        File file = new File(SchematicaDownload.tokenPath);

        file.createNewFile();
    }

    private void createDefaultLinkFile() throws IOException {
        File file = new File(SchematicaDownload.linkPath);

        file.createNewFile();
    }

    private void loadToken() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(SchematicaDownload.tokenPath));
        SchematicaDownload.token = reader.readLine();

        reader.close();
    }

    private void loadLinks() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(SchematicaDownload.linkPath));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] link = line.split(" ");

            DiscordManager.putLink(UUID.fromString(link[0]), Long.parseLong(link[1]));
        }

        reader.close();
    }

    private void saveLinks() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(SchematicaDownload.linkPath));

        for (Map.Entry<UUID, Long> entry : DiscordManager.getLinks()) {
            writer.write(entry.getKey().toString());
            writer.write(' ');
            writer.write(entry.getValue().toString());
            writer.write('\n');
        }

        writer.flush();
        writer.close();
    }

}
