/*
 * #%L
 * Netarchivesuite - deploy - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.deploy;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

public class CompleteSettingsTester extends TestCase {
    @Override
    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    @Override
    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);
    }

    /**
     * Rebuilds the file src/dk/netarkivet/deploy/default_settings.xml. Eg. this is not a real test.
     */

    public void testCompleteSettings() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("");
        File file = new File(url.toURI());
        // ToDo The generation of the complete settings file should be moved
        // to the build functionality directly.
        File settingsFile = new File(file, "dk/netarkivet/deploy/complete_settings.xml");
        BuildCompleteSettings.buildCompleteSettings(settingsFile.getPath());
    }
}
