/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.common.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the Settings class.
 * 
 */
public class SettingsTester extends TestCase  {
    ReloadSettings rs = new ReloadSettings(new File(TestInfo.SETTINGSFILENAME));

    public SettingsTester(String sTestName) {
        super(sTestName);
    }

    public void setUp()  {
        rs.setUp();
        try {
            if (!TestInfo.TEMPDIR.exists()) {
                TestInfo.TEMPDIR.mkdir();
            }
            FileUtils.removeRecursively(TestInfo.TEMPDIR);
            TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        }
        catch (Exception e) {
            fail("Could not setup configuration");
        }

    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        rs.tearDown();
    }

    /**
     * Verify the simple read and write functionality.
     */
    public void testReadAndWrite() {
                
        String processTimeout = Settings.get(CommonSettings.PROCESS_TIMEOUT);
        String port = Settings.get(TestInfo.PORT);
        
        assertEquals("Must match xml data", TestInfo.PROCESS_TIMEOUT_VALUE, processTimeout);
        assertEquals("Must match xml data", TestInfo.PORTVALUE, port );

        Settings.set(TestInfo.TIMEOUT, "42");
        assertEquals("Must value just added", Settings.get(TestInfo.TIMEOUT), "42");

        Settings.set(CommonSettings.PROCESS_TIMEOUT, "default_2");
        processTimeout = Settings.get(CommonSettings.PROCESS_TIMEOUT);
        assertEquals("Must match new value", "default_2", processTimeout);

        // verify that properties settings override loaded settings
        Settings.set(TestInfo.UNUSED_PROPERTY, "first value");
        assertEquals("Must match old value", "first value",
                     Settings.get(TestInfo.UNUSED_PROPERTY));
        System.setProperty(TestInfo.UNUSED_PROPERTY, "override");
        processTimeout = Settings.get(TestInfo.UNUSED_PROPERTY);
        assertEquals("Must match new value", "override", processTimeout);
    }

    /**
     * Test that reload functionality resets settings to original values.
     */
    public void testReload() {
        String setting = CommonSettings.NOTIFICATIONS_CLASS;
        String backup_value = Settings.get(setting);
        Settings.set(setting, "hello world");
        assertEquals("Failed to change setting: ",
                     Settings.get(setting), "hello world");
        Settings.reload();
        assertEquals("Failed to reset settings: ",
                     Settings.get(setting), backup_value);
    }

    /**
     * Test that setting a non-existent key will just add it.
     */
    public void testSetNonExistentKey() {
        Settings.set("settings.no.such.key", "hest");
        assertEquals("Should have new value", "hest",
                     Settings.get("settings.no.such.key"));
    }

    /**
     * Test that creating a key that already exists throws an exception.
     */
    public void testCreatePreExistingKey() {
        Settings.set(CommonSettings.CACHE_DIR, "hest");
        assertEquals("Should have new value", "hest",
                     Settings.get(CommonSettings.CACHE_DIR));
    }

    /**
     * Test that getAll can get a value set in a system property.
     */
    public void testGetAllFromSysprop() {
        String[] val = Settings.getAll(Settings.SETTINGS_FILE_PROPERTY);
        assertTrue("Expected single value but got " + val.length, val.length == 1);
        assertEquals("Value was not as expected: ", TestInfo.SETTINGSFILENAME, val[0]);
    }

    /**
     * Test that we can get an array of strings for a key value.
     */
    public void testGetAllFromSetting() {
        String key = "settings.for.test.purposes";
        Settings.set(key, "d");
        Settings.set(key, "1", "2");
        String[] val2 = Settings.getAll(key);
        assertTrue("Expected two value but got " + val2.length, val2.length == 2);
        assertEquals("Unexpected value: ", "1", val2[0]);
        assertEquals("Unexpected value: ", "2", val2[1]);
    }

    /**
     * Test that getAll fails for an unknown key.
     */
    public void testGetAllFailure() {
        String key = "just.for.test.purposes2";
        try {
            Settings.getAll(key);
            fail("Should throw UnknownID for invented key " + key);
        } catch (UnknownID e) {
            //expected
        }
    }

    /**
     * Test getLong works.
     */
    public void testGetLongWorks() {
       String key = "settings.for.test.purposes3";
        long val = 6961464186L;
        Settings.set(key, new String[]{Long.toString(val)});
        assertEquals("Did not return set value: ", Settings.getLong(key), val);
    }

    /**
     * Test that getLong fails when value cannot be parsed as long
     */
    public void testGetLongFails() {
        String key = "settings.for.test.purposes4";
        float val = 3.1415f;
        Settings.set(key, new String[]{Float.toString(val)});
        try {
            long l = Settings.getLong(key);
            fail("Should throw ArgumentNotValid, not return " + l);
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
    
    /**
     * Test that getBoolean returns true, when it can be parsed as 
     * some upper/lowercase combination of the string "true"
     */
    public void testGetBoolean() {
        String key = "settings.for.test.purposes5";
        String trueAsString = "True";
        Settings.set(key, new String[]{trueAsString});
        try {
            boolean b = Settings.getBoolean(key);
            if (!b) {
                fail("Should have been parsed as true, but was parsed as false");
            }
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid on valid boolean string"
                    + e);
        }
    }
    /*
    public void testValidateWithXSD() throws Exception {
        String settingsFileProperty = Settings.SETTINGS_FILE_PROPERTY;
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-full.xml").getAbsolutePath());
        XmlUtils.validateWithXSD(new File(
                "./lib/data-definitions/settings.xsd"));
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-generated.xml").getAbsolutePath());
        XmlUtils.validateWithXSD(new File(
                "./lib/data-definitions/settings.xsd"));
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-bad-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            XmlUtils.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
            fail("Should have failed XSD validation on xml with wrong type entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-missing-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            XmlUtils.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
            fail("Should have failed XSD validation on xml with missing entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-extra-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            XmlUtils.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
            fail("Should have failed XSD validation on xml with extra entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    public void testValidateStrings() throws Exception {
        String settingsFileProperty = Settings.SETTINGS_FILE_PROPERTY;
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-full.xml").getAbsolutePath());
        Settings.reload();
        validateStrings(CommonSettings.class,
                                 Arrays.asList(
            "DEFAULT_SETTINGS_CLASSPATH"));
        // Should throw no exceptions on a generated Settings file.
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-missing-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            validateStrings(CommonSettings.class,
                                     Arrays.asList(
            "DEFAULT_SETTINGS_CLASSPATH"));
            fail("Should have failed string validation on xml with missing entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        // Should throw no exceptions on the standard Settings file.
        System.setProperty(settingsFileProperty,
                new File(TestInfo.TEMPDIR, "settings-generated.xml").getAbsolutePath());
        Settings.reload();
        validateStrings(CommonSettings.class,
                                 Arrays.asList(
            "DEFAULT_SETTINGS_CLASSPATH"));
    }
    
    */
    
    /**
     * Validate that the strings defined in the given class are present in
     * the settings xml file.
     * Checks all static String fields that are not explicitly excluded above.
     * This asserts the correspondence between the settings we think we have
     * and those defined in the XSD/.xml file.
     *
     * @param classToCheck   The class defining the constants to check
     * @param excludedFields Fields not to check, even though they are constants
     *                       in that class.
     */
    public static void validateStrings(Class classToCheck,
                                List<String> excludedFields) {
        ArgumentNotValid.checkNotNull(classToCheck, "Class classToCheck");
        ArgumentNotValid.checkNotNull(
                    excludedFields,
                    "List<String> excludedFields");
        Field[] fields = classToCheck.getDeclaredFields();
        for (Field f : fields) {
            if (!excludedFields.contains(f.getName())
                    && f.getType().equals(String.class) && Modifier
                    .isStatic(f.getModifiers())) {
                String xmlKey;
                try {
                    xmlKey = (String) f.get(null);
                } catch (IllegalAccessException e) {
                    final String msg
                            = "Internal error while checking settings for key '"
                              + f.getName() + "' ";
                    throw new ArgumentNotValid(msg, e);
                }
                try {
                    Settings.get(xmlKey);
                } catch (UnknownID e) {
                    final String msg = "Setting '" + xmlKey + "' ('"
                            + f.getName() + "') is undefined in '"
                            + Settings.getSettingsFiles() + "'";
                    throw new ArgumentNotValid(msg, e);
                }
            }
        }
    }
}