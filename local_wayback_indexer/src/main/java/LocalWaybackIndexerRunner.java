import java.io.File;
import java.net.URISyntaxException;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.indexer.WaybackIndexerApplication;

public class LocalWaybackIndexerRunner {

    public static void main(String[] args) throws URISyntaxException {
        File settingsFile = new File(
                Thread.currentThread().getContextClassLoader().getResource("settings_WaybackIndexerApplication.xml")
                        .toURI());
        File logbackFile = new File(
                Thread.currentThread().getContextClassLoader().getResource("logback.xml")
                        .toURI());
        System.setProperty(Settings.SETTINGS_FILE_PROPERTY, settingsFile.getAbsolutePath());
        System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());
        WaybackIndexerApplication.main(args);
    }

}
