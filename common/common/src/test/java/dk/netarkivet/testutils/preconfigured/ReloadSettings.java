package dk.netarkivet.testutils.preconfigured;

import java.io.File;

import dk.netarkivet.common.utils.Settings;

public class ReloadSettings implements TestConfigurationIF {
    private File f;
    private String oldSettingsFilenames;

    public ReloadSettings() {

    }

    public ReloadSettings(File f) {
        this.f = f;
    }

    public void setUp() {
        oldSettingsFilenames = System.getProperty(Settings.SYSTEM_PROPERTY);
        if (f != null) {
            System.setProperty(Settings.SYSTEM_PROPERTY, f.getAbsolutePath());
        }
        Settings.reload();
    }

    public void tearDown() {
        if (oldSettingsFilenames != null) {
            System.setProperty(Settings.SYSTEM_PROPERTY, oldSettingsFilenames);
        } else {
            System.clearProperty(Settings.SYSTEM_PROPERTY);
        }
        Settings.reload();
    }

}
