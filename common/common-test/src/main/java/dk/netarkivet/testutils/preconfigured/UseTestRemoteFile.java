/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

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
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_FILE, new File(
                    dk.netarkivet.common.distribute.TestInfo.ORIGINALS_DIR, "testkeystore").getPath());
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_FILE, new File(
                    dk.netarkivet.common.distribute.TestInfo.ORIGINALS_DIR, "testkeystore").getPath());
        }
        try {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_PASSWORD, "testpass");
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_PASSWORD, "testpass");
        }

        try {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEY_PASSWORD, "testpass2");
        } catch (ArgumentNotValid e) {
            Settings.set(HTTPSRemoteFile.HTTPSREMOTEFILE_KEY_PASSWORD, "testpass2");
        }
    }

    public void tearDown() {
        TestRemoteFile.removeRemainingFiles();
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, originalRemoteFileClass);
    }
}
