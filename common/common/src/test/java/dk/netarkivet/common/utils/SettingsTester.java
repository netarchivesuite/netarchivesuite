/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
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
     * Verify the simple read and write functionality
     */
    public void testReadAndWrite() {
        String defaultseed = Settings.get(TestInfo.DEFAULTSEEDLIST);
        String port = Settings.get(TestInfo.PORT);

        assertEquals("Must match xml data", TestInfo.DEFAULTSEEDLIST_VALUE, defaultseed  );
        assertEquals("Must match xml data", TestInfo.PORTVALUE, port );

        String[] values2 = new String[]{"42"};
        Settings.set(TestInfo.TIMEOUT, values2);
        assertEquals("Must value just added", Settings.get(TestInfo.TIMEOUT), "42" );

        String[] values = new String[]{"default_2"};
        Settings.set(TestInfo.DEFAULTSEEDLIST, values);
        defaultseed = Settings.get(TestInfo.DEFAULTSEEDLIST);
        assertEquals("Must match new value", "default_2", defaultseed );

        // verify that properties settings override loaded settings
        String[] values1 = new String[]{"first value"};
        Settings.set(TestInfo.UNUSED_PROPERTY, values1);
        assertEquals("Must match old value", "first value",
                     Settings.get(TestInfo.UNUSED_PROPERTY));
        System.setProperty(TestInfo.UNUSED_PROPERTY, "overide" );
        defaultseed = Settings.get(TestInfo.UNUSED_PROPERTY);
        assertEquals("Must match new value", "overide", defaultseed );
    }

    /**
     * Test that reload functionality resets settings to original values
     */
    public void testReload(){
        String backup_value = Settings.get(HarvesterSettings.DEFAULT_SEEDLIST);
        String[] values = new String[]{"hello world"};
        Settings.set(HarvesterSettings.DEFAULT_SEEDLIST, values);
        assertEquals("Failed to change setting: ",
                     Settings.get(HarvesterSettings.DEFAULT_SEEDLIST), "hello world");
        Settings.reload();
        assertEquals("Failed to reset settings: ",
                     Settings.get(HarvesterSettings.DEFAULT_SEEDLIST), backup_value);
    }

    /**
     * Test that setting a non-existent key will just add it
     */
    public void testSetNonExistentKey() {
        String[] values = new String[]{"hest"};
        Settings.set("settings.no.such.key", values);
        assertEquals("Should have new value", "hest",
                     Settings.get("settings.no.such.key"));
    }

    /**
     * Test that creating a key that already exists throws an exception
     */
    public void testCreatePreExistingKey() {
        String[] values = new String[]{"hest"};
        Settings.set(HarvesterSettings.DOMAIN_CONFIG_MAXRATE, values);
        assertEquals("Should have new value", "hest",
                     Settings.get(HarvesterSettings.DOMAIN_CONFIG_MAXRATE));
    }

    /**
     * Test that getAll can get a value set in a system property
     */
    public void testGetAllFromSysprop() {
        String[] val = Settings.getAll(Settings.SYSTEM_PROPERTY);
        assertTrue("Expected singke value but got " + val.length, val.length == 1);
        assertEquals("Value was not as expected: ", TestInfo.SETTINGSFILENAME, val[0]);
    }

    /**
     * Test that we can get an array of strings for a key value
     */
    public void testGetAllFromSetting() {
        String key = "settings.for.test.purposes";
        String [] val = {"1", "2"};
        String[] values = new String[]{"d"};
        Settings.set(key, values);
        Settings.set(key, val);
        String[] val2 = Settings.getAll(key);
        assertTrue("Expected two value but got " + val2.length, val2.length == 2);
        assertEquals("Unexpected value: ", val[0], val2[0]);
        assertEquals("Unexpected value: ", val[1], val2[1]);
    }

    /**
     * Test that getAll fails for an unknown key
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
     * Test getLong works
     */
    public void testGetLongWorks() {
       String key = "settings.for.test.purposes3";
        long val = 6961464186L;
        String[] values = new String[]{""+val};
        Settings.set(key, values);
        assertEquals("Did not return set value: ", Settings.getLong(key), val);
    }

    /**
     * Test that getLong fails when value cannot be parsed as long
     */
    public void testGetLongFails() {
        String key = "settings.for.test.purposes4";
        float val = 3.1415f;
        String[] values = new String[]{""+val};
        Settings.set(key, values);
        try {
            long l = Settings.getLong(key);
            fail("Should throw ArgumentNotValid, not return " + l);
        } catch (ArgumentNotValid e) {
            //expected
        }
    }

    public void testValidateWithXSD() throws Exception {
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-full.xml").getAbsolutePath());
        Settings.validateWithXSD(new File(
                "./lib/data-definitions/settings.xsd"));
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-generated.xml").getAbsolutePath());
        Settings.validateWithXSD(new File(
                "./lib/data-definitions/settings.xsd"));
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-bad-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            Settings.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
            fail("Should have failed XSD validation on xml with wrong type entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-missing-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            Settings.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
            fail("Should have failed XSD validation on xml with missing entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-extra-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            Settings.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
            fail("Should have failed XSD validation on xml with extra entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    public void testValidateStrings() throws Exception {
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-full.xml").getAbsolutePath());
        Settings.reload();
        Settings.validateStrings(CommonSettings.class,
                                                CommonSettings.EXCLUDED_FIELDS);
        // Should throw no exceptions on a generated Settings file.
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-missing-entry.xml").getAbsolutePath());
        Settings.reload();
        try {
            Settings.validateStrings(CommonSettings.class,
                                                    CommonSettings.EXCLUDED_FIELDS);
            fail("Should have failed string validation on xml with missing entry");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        // Should throw no exceptions on the standard Settings file.
        System.setProperty(Settings.SYSTEM_PROPERTY,
                new File(TestInfo.TEMPDIR, "settings-generated.xml").getAbsolutePath());
        Settings.reload();
        Settings.validateStrings(CommonSettings.class,
                                                    CommonSettings.EXCLUDED_FIELDS);
    }
}