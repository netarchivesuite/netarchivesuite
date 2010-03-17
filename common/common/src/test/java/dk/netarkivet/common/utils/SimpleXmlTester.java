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
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the class SimpleXml.
 */
public class SimpleXmlTester extends TestCase {
    public SimpleXmlTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        try {
            if (!dk.netarkivet.common.utils.TestInfo.TEMPDIR.exists()) {
                TestInfo.TEMPDIR.mkdir();
            }
            FileUtils.removeRecursively(TestInfo.TEMPDIR);
            TestFileUtils.copyDirectoryNonCVS(
                    TestInfo.DATADIR,
                    dk.netarkivet.common.utils.TestInfo.TEMPDIR);
        }
        catch (Exception e) {
            fail("Could not setup configuration");
        }
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
    }

    /** Verify that it is possible to load a simple XML file author. */
    public void testLoadAndSave() {
        SimpleXml xml = new SimpleXml(TestInfo.TESTXML);
        String value = xml.getString("dk.netarkivet.test.q");
        assertEquals("Loaded value must exist", "what is the question", value);

        List<String> items = xml.getList("dk.netarkivet.test.list1");
        CollectionAsserts.assertListEquals("Must match stored items",
                                           items, "item1", "item2", "item3");

        xml.save(TestInfo.NEWXML);
        SimpleXml newxml = new SimpleXml(TestInfo.NEWXML);
        String newvalue = newxml.getString("dk.netarkivet.test.q");
        assertEquals("Reloaded value must exist",
                     "what is the question", newvalue);

        List<String> newitems = newxml.getList("dk.netarkivet.test.list1");
        CollectionAsserts.assertListEquals("Must match stored items",
                                           newitems, "item1", "item2", "item3");

        List<String> answers = xml.getList("dk.netarkivet.answer");
        CollectionAsserts.assertListEquals("Must have both answers",
                                           answers, "42", "43");
    }

    /** Verify that it is possible to delete and set a key in a simple XML file. */
    public void testDeleteAndSet() {
        SimpleXml xml = new SimpleXml(TestInfo.TESTXML);

        /** add a key and value */
        String value = "unknown";
        xml.add("dk.netarkivet.user", value);

        /** verify that the key exists */
        assertTrue("Key must exist after adding",
                   xml.hasKey("dk.netarkivet.user"));

        /** verify that the key and value exists */
        String retrievevalue = xml.getString("dk.netarkivet.user");
        assertEquals("Inserted key should be correct", "unknown",
                     retrievevalue);
    }

    /** Check loading of non xml file fails. */
    public void testLoadNonXml() {
        try {
            new SimpleXml(TestInfo.INVALIDXML);
            fail("Loading invalid xml should throw exception");
        } catch (IOFailure e) {
            //Expected
        }

    }

    /** Check loading of unknown file or null file. */
    public void testLoadNonExistingFile() {
        try {
            new SimpleXml((File) null);
            fail("Loading null file should throw an exception");
        } catch (Exception e) {
            // ok
        }

        try {
            new SimpleXml(
                    new File("./tests/dk/netarkivet/utils/data/invalid.xml"));
            fail("Loading unknown file should throw an exception");
        } catch (Exception e) {
            // ok
        }
    }


    public void testAdd() {
        SimpleXml simpleXml = new SimpleXml(TestInfo.SETTINGS_FILE);
        String key = "settings.test.key";
        String value = "a value";
        assertFalse("Key should not exist before adding",
                    simpleXml.hasKey(key));
        try {
            simpleXml.add(null, value);
            fail("Should die on null key");
        } catch (ArgumentNotValid e) {
            //expected
        }
        try {
            simpleXml.add("", value);
            fail("Should die on empty key");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            simpleXml.add(key, (String[]) null);
            fail("Should die on null value");
        } catch (ArgumentNotValid e) {
            //expected
        }
        assertFalse("Key should not exist after errors",
                    simpleXml.hasKey(key));

        simpleXml.add(key, value);
        assertTrue("Key should exist after adding",
                   simpleXml.hasKey(key));
        assertEquals("Key's value should be the one set",
                     value, simpleXml.getString(key));

        String value2 = "another value";
        simpleXml.add(key, value2);
        simpleXml.add(key, value2);
        assertTrue("Key should exist after adding",
                   simpleXml.hasKey(key));
        assertEquals("Key's value should be the one set first",
                     value, simpleXml.getString(key));

        CollectionAsserts.assertListEquals(
                "Should have values in order after second insert",
                simpleXml.getList(key), value, value2, value2);
    }

    public void testUpdate() {
        SimpleXml simpleXml = new SimpleXml(TestInfo.SETTINGS_FILE);
        String key = "settings.test.reset";
        String value1 = "first value";
        simpleXml.add(key, value1);
        simpleXml.add(key, value1);

        String value2 = "second value";
        simpleXml.update(key, value2);
        CollectionAsserts.assertListEquals("Should have updated value only",
                                           simpleXml.getList(key), value2);

        try {
            simpleXml.update(key, (String[]) null);
            fail("Should die on null value");
        } catch (ArgumentNotValid e) {
            CollectionAsserts.assertListEquals("Should still have old value",
                                               simpleXml.getList(key), value2);
        }

        try {
            simpleXml.update(key, value1, null);
            fail("Should die on null value");
        } catch (ArgumentNotValid e) {
            CollectionAsserts.assertListEquals("Should still have old value",
                                               simpleXml.getList(key), value2);
        }

        simpleXml.update(key);
        assertFalse("Should not have key any more", simpleXml.hasKey(key));
    }

    public void testHasKey() {
        SimpleXml simpleXml = new SimpleXml(TestInfo.SETTINGS_FILE);
        assertTrue("Should have known key",
                   simpleXml.hasKey("settings.common.tempDir"));
        assertFalse("Should not have unknown key",
                    simpleXml.hasKey("an.unknown.key"));
        assertFalse("Should not have unknown key under root element",
                    simpleXml.hasKey("settings.foo.bar"));
        try {
            simpleXml.hasKey(null);
            fail("Should fail on null key");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            simpleXml.hasKey("");
            fail("Should fail on empty key");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    public void testGetTree() {
        SimpleXml xml = new SimpleXml(TestInfo.TESTXML);
        StringTree<String> tree1 = xml.getTree("dk.netarkivet.test.q");
        assertNotNull("Getting a tree should return non-null",
                      tree1);
        assertEquals("Single-leaf tree should just have contents",
                     "what is the question", tree1.getValue());

        StringTree<String> tree2 = xml.getTree("dk.netarkivet.test");
        assertNotNull("Getting a tree should return non-null",
                      tree2);
        assertEquals("Complex tree should have simple sub-tree",
                     "what is the question", tree2.getSubTree("q").getValue());
        CollectionAsserts.assertListEquals(
                "Complex tree should have complex sub-tree",
                tree2.getLeafMultimap().get("list1"),
                "item1", "item2", "item3");

        try {
            xml.getTree("foo.bar");
            fail("Should get exception on non-existing path");
        } catch (UnknownID e) {
            // Expected
        }

        try {
            xml.getTree("dk.netarkivet");
            fail("Should get exception on ambiguous path");
        } catch (UnknownID e) {
            // Expected
        }
    }
}