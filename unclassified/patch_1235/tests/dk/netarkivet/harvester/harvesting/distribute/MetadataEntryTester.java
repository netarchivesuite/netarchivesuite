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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Tests for class MetadataEntry.
 *
 */
public class MetadataEntryTester extends TestCase {
    private String aRealURL;
    private String anEmptyURL;
    private String aNullURL = null;

    private String aRealMimetype;
    private String anEmptyMimetype;
    private String aNullMimetype = null;

    private String realData;
    private String emptyData;
    private String nullData = null;

    public void setUp() {
        aRealURL = "metadata://netarkivet.dk/crawl/"
                + "setup/order.xml?version=1.7.1&harvestid=1&jobid=1";
        aRealMimetype = "text/plain";
        realData = "bla bla bla bla bla bla bla bla "
            + "a bla bla bla bla bla bla bla bla bla bla bla";
        anEmptyURL = "";
        anEmptyMimetype = "";
        emptyData = "";
    }

    /*
     * Test method for 'dk.netarkivet.distribute.scheduler.MetadataEntry.MetadataEntry(String, String, String)'
     */
    public void testMetadataEntry() {

        // check, that null & empty arguments are not accepted. (1)
        try {
            new MetadataEntry(aNullURL, aRealMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

       // check, that null & empty arguments are not accepted. (2)
        try {
            new MetadataEntry(anEmptyURL, aRealMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that null & empty arguments are not accepted. (3)
        try {
            new MetadataEntry(aRealURL, aNullMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that null & empty arguments are not accepted. (4)
        try {
            new MetadataEntry(aRealURL, anEmptyMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }
        // check, that null & empty arguments are not accepted. (5)
        try {
            new MetadataEntry(aRealURL, aRealMimetype, nullData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }
        // check, that null & empty arguments are not accepted. (6)
        try {
            new MetadataEntry(aRealURL, aRealMimetype, emptyData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that valid arguments are accepted.
        try {
            new MetadataEntry(aRealURL, aRealMimetype, realData);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected for valid arguments");
        }
    }

    /*
     * Test method for getMethods
     */
    public void testGetterAndSetters() {
       MetadataEntry md = new MetadataEntry(aRealURL, aRealMimetype, realData);
       assertEquals("getData() returns wrong value", realData, new String(md.getData()));
       assertEquals("getURL() returns wrong value", aRealURL, md.getURL());
       assertEquals("getMimeType() returns wrong value", aRealMimetype, md.getMimeType());
    }

    /**
     * Test isDuplicateReductionMetadataEntry
     */
     public void testIsDuplicateReductionMetadataEntry() {
         MetadataEntry md = new MetadataEntry("metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=%s&harvestnum=%s&jobid=%s", aRealMimetype, realData);
         MetadataEntry md1 = new MetadataEntry("metadata://netarkivet.dk/crawl/setup/aliases?majorversion=1&minorversion=0&harvestid=%s&harvestnum=%s&jobid=%s", aRealMimetype, realData);
         assertFalse("md1 should not be recognized as a duplicatereduction metadataEntry", md1.isDuplicateReductionMetadataEntry());
         assertTrue("md should be recognized as a duplicatereduction metadataEntry", md.isDuplicateReductionMetadataEntry());
     }

    /*
     * Test serializability
     */
    public void testSerializability() throws IOException, ClassNotFoundException {
        //Take an object:

        MetadataEntry fooOriginal = new MetadataEntry(aRealURL, aRealMimetype, realData);
        //serialize and deserialize the study object:
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(fooOriginal);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        MetadataEntry fooCopy;
        fooCopy = (MetadataEntry) ois.readObject();
        //Finally, compare their visible states:
        assertEquals("After serialization the states differed:\n" +
                relevantState(fooOriginal) + "\n" + relevantState(fooCopy),relevantState(fooOriginal),relevantState(fooCopy));
    }

    private String relevantState(MetadataEntry fooOriginal) {
        return new String(fooOriginal.getURL() + fooOriginal.getMimeType()
                + new String(fooOriginal.getData()));
    }
}
