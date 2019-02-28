import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.indexer.WaybackIndexerApplication;

public class LocalWaybackIndexerRunner {

    public static void main(String[] args) throws URISyntaxException, IOException {

        //Local port forward that auto-closes when no longer used
        // http://www.g-loaded.eu/2006/11/24/auto-closing-ssh-tunnels/
        Process myProcess = new ProcessBuilder("ssh",
                "-f",
                "-o", "ExitOnForwardFailure=yes",
                "-L", "7676:kb-test-adm-001.kb.dk:7676",
                "-L", "33700:kb-test-adm-001.kb.dk:33700",
                "netarkdv@sb-test-har-001.statsbiblioteket.dk",
                "sleep 10")
                .start();

        File settingsFile = getResourceFile("settings_WaybackIndexerApplication.xml");
        System.setProperty(Settings.SETTINGS_FILE_PROPERTY, settingsFile.getAbsolutePath());

        File logbackFile = getResourceFile("logback.xml");
        System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());

        WaybackIndexerApplication.main(args);
    }

    private static File getResourceFile(String name) throws FileNotFoundException, URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (resource == null){
            throw new FileNotFoundException(name+" not found");
        }
        return new File(resource.toURI());
    }

}
