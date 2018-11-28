/*
 * #%L
 * Netarchivesuite - deploy - test
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
package dk.netarkivet.deploy;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestResourceUtils;

public class CompleteSettingsTester {
    @Rule public TestName test = new TestName();
    private File WORKING_DIR;

    @Before
    public void initialize() {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
    }

    @Test
    public void testCompleteSettings() {
        BuildCompleteSettings.buildCompleteSettings(WORKING_DIR.getPath() + "/complete_settings.xml");
    }
    
    @Test
    /** This uses the BuildCompleteSettings.defaultCompleteSettingsPath if no arg given to BuildCompleteSettings.main() */ 
    public void testCompleteSettingsWithNoArg() {
        String defaultCompleteSettingsPath = BuildCompleteSettings.defaultCompleteSettingsPath;
        BuildCompleteSettings.buildCompleteSettings(defaultCompleteSettingsPath);
        FileUtils.remove(new File(defaultCompleteSettingsPath));
    }
    
    @Test
    public void testNewlyGeneratedWithExistingDefaults() throws IOException {
        File newCompleteSettingsFile = new File(WORKING_DIR.getPath() + "/complete_settings.xml");
        BuildCompleteSettings.buildCompleteSettings(newCompleteSettingsFile.getAbsolutePath());
        File existingCompleteSettingsFile = new File("../deploy-core/src/main/resources/" + Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
        // compare filesizes.
        if (newCompleteSettingsFile.length() != existingCompleteSettingsFile.length()) {
            fail("The newly generated completesettings has size (" +  newCompleteSettingsFile.length() 
                    + ") which is different from the size of the existing one (" +  existingCompleteSettingsFile.length() + "). Update the file '" + existingCompleteSettingsFile.getCanonicalPath() 
                    + "' by following the procedure in deploy/deploy-core/src/main/java/dk/netarkivet/deploy/BuildCompleteSettings.java");
        }
    }
    
}
