package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.common.Settings;

public class ReloadSettings implements TestConfigurationIF {
    public void setUp() {
        Settings.reload();
    }

    public void tearDown() {
        Settings.reload();
    }

}
