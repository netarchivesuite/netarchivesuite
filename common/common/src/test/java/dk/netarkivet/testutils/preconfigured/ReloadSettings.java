package dk.netarkivet.testutils.preconfigured;

import java.io.File;

import dk.netarkivet.common.utils.Settings;

public class ReloadSettings implements TestConfigurationIF {
    private File f;
    private String oldSettingsFilenames;
    private String settingsFileProperty = Settings.SETTINGS_FILE_PROPERTY;

    public ReloadSettings() {

    }

    public ReloadSettings(File f) {
        this.f = f;
    }

    public void setUp() {
        oldSettingsFilenames = System.getProperty(settingsFileProperty);
        if (f != null) {
            System.setProperty(settingsFileProperty, f.getAbsolutePath());
        }
        Settings.reload();
    }

    public void tearDown() {
        if (oldSettingsFilenames != null) {
            System.setProperty(settingsFileProperty, oldSettingsFilenames);
        } else {
            System.clearProperty(settingsFileProperty);
        }
        Settings.reload();
    }

}
