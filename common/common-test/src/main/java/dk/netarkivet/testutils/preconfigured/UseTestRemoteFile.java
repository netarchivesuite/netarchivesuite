
package dk.netarkivet.testutils.preconfigured;

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.HTTPRemoteFile;
import dk.netarkivet.common.distribute.HTTPSRemoteFile;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;

/**
 * A preconfigure class for using TestRemoteFile instead of FTP
 *
 */

public class UseTestRemoteFile implements TestConfigurationIF {
    private String originalRemoteFileClass;

    public void setUp() {
        originalRemoteFileClass = Settings.get(CommonSettings.REMOTE_FILE_CLASS);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, TestRemoteFile.class.getName());
        try {
            Settings.set(HTTPRemoteFile.HTTPREMOTEFILE_PORT_NUMBER, Integer.toString(5442));
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPRemoteFile.HTTPREMOTEFILE_PORT_NUMBER, Integer.toString(5442));
        }
        try {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_FILE, 
                    new File(dk.netarkivet.common.distribute.TestInfo.ORIGINALS_DIR, 
                            "testkeystore").getPath());
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_FILE,
                    new File(dk.netarkivet.common.distribute.TestInfo.ORIGINALS_DIR, 
                            "testkeystore").getPath());
        }
        try {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_PASSWORD,
                         "testpass");
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_PASSWORD,
                         "testpass");
        }

        try {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEY_PASSWORD,
                         "testpass2");
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEY_PASSWORD,
                         "testpass2");
        }
    }

    public void tearDown() {
        TestRemoteFile.removeRemainingFiles();
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, originalRemoteFileClass);
    }
}
