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
package dk.netarkivet.harvester.datamodel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.testutils.StringAsserts;


/** Test-class for AliasInfo class. */
public class AliasInfoTester extends TestCase {

    String nullString = null;
    String emptyString = "";
    Date nullDate = null;
    Date realDate = new Date();
    String aliasDomain = "netarkivet.dk";
    String afatherDomain = "kb.dk";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'dk.netarkivet.datamodel.AliasInfo.AliasInfo(String, String, Date)'
     * Checks, that ArgumentNotValid exception is thrown when
     * either of the given Domain, the aliasOf, the lastChange attributes is null or empty.
     */
    public void testAliasInfo() {


        // Check that allowed arguments are accepted
        try {
            new AliasInfo(aliasDomain, afatherDomain, realDate);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid not expected with valid arguments given to constructor");
        }


        // check that null or empty arguments are not accepted.
        try {
            new AliasInfo(nullString, afatherDomain, realDate);
            fail("ArgumentNotValid expected with null 'domain' argument given to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            new AliasInfo(emptyString, afatherDomain, realDate);
            fail("ArgumentNotValid expected with empty 'domain' argument given to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            new AliasInfo(aliasDomain, nullString, realDate);
            fail("ArgumentNotValid expected with null 'aliasOf' given to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            new AliasInfo(aliasDomain, emptyString, realDate);
            fail("ArgumentNotValid expected with empty 'aliasOf' given to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            new AliasInfo(aliasDomain, afatherDomain, nullDate);
            fail("ArgumentNotValid expected with null 'lastChange' given to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            new AliasInfo(aliasDomain, emptyString, realDate);
            fail("ArgumentNotValid expected with empty 'aliasOf' given to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that !domain.equals(aliasOf)
        try {
            new AliasInfo(aliasDomain,aliasDomain, realDate);
            fail("ArgumentNotValid expected with domain == aliasOf");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /*
     * Test get and set-methods.
     */
    public void testSetAndGetters() {
       AliasInfo ai = new AliasInfo(aliasDomain,afatherDomain, realDate);
       assertEquals("getDomain returns wrong value", aliasDomain, ai.getDomain());
       assertEquals("getAliasOf returns wrong value", afatherDomain, ai.getAliasOf());
       assertTrue("getLastChange returns wrong value",
               realDate.equals(ai.getLastChange()));
      }

    /***
     * tests makeAliasMetadataEntry(List<AliasInfo> aliases, String URL, String mimetype).
     *
     */
    public void testMakeAliasMetadataEntries() {
        List<AliasInfo> emptyAliases = new ArrayList<AliasInfo>();
        List<AliasInfo> fiveAliases = new ArrayList<AliasInfo>();
        List<AliasInfo> expiredAliases = new ArrayList<AliasInfo>();
        Date expiredDate1 = new Date(System.currentTimeMillis()
                - Constants.ALIAS_TIMEOUT_IN_MILLISECONDS
                - 1000L);
        Date expiredDate2 = new Date(System.currentTimeMillis()
                - Constants.ALIAS_TIMEOUT_IN_MILLISECONDS
                - 500L);
        Date nonExpiredDate = new Date();
        fiveAliases.add(new AliasInfo(aliasDomain, afatherDomain, realDate));
        fiveAliases.add(new AliasInfo("aliasdomain.dk", "fatherdomain.dk", realDate));
        fiveAliases.add(new AliasInfo("aliasdomain1.dk", "fatherdomain1.dk", expiredDate1));
        fiveAliases.add(new AliasInfo("aliasdomain2.dk", "fatherdomain2.dk", expiredDate2));
        fiveAliases.add(new AliasInfo("aliasdomain3.dk", "fatherdomain3.dk", nonExpiredDate));
        expiredAliases.add(new AliasInfo("aliasdomain1.dk", "fatherdomain1.dk", expiredDate1));
        expiredAliases.add(new AliasInfo("aliasdomain2.dk", "fatherdomain2.dk", expiredDate2));

        MetadataEntry meta = null;
        // Tests for valid arguments:
        // List<AliasInfo> aliases, Long origHarvestDefinitionID, int harvestNum, Long jobId)
        Long jobId = new Long(42L);
        int harvestNum = 1;
        Long origHarvestDefinitionID = new Long(2L);
        try {
            MetadataEntry.makeAliasMetadataEntry(null, null, 0, null);
            fail("Exception expected with null arguments");
        } catch (Exception e) {
            // Expected
        }
        try {
            MetadataEntry.makeAliasMetadataEntry(emptyAliases, null, 0, null);
            fail("Exception expected with null arguments");
        } catch (Exception e) {
            // Expected
        }

        try {
            MetadataEntry.makeAliasMetadataEntry(emptyAliases, origHarvestDefinitionID,
                    harvestNum, null);
            fail("Exception expected with null arguments");
        } catch (Exception e) {
            // Expected
        }

        try {
            meta = MetadataEntry.makeAliasMetadataEntry(emptyAliases, origHarvestDefinitionID, harvestNum, jobId);
            //meta = MetadataEntry.makeAliasMetadataEntry(fiveAliases, "URL is not valid", "text/plain");
        } catch (Exception e) {
            fail("Exception not expected with valid arguments");
        }
        assertEquals("makeAliasMetadataEntry should return null with empty list of aliases", null, meta);

        /** add check that it does not add expired aliases to the metadataEntry */
        meta = MetadataEntry.makeAliasMetadataEntry(fiveAliases, origHarvestDefinitionID, harvestNum, jobId);
        // this MetadataEntry should contain URL=metadata://netarkivet/crawl, mimetype=text/plain
        // and data should contain strings: "netarkivet.dk is an alias for kb.dk", "aliasdomain.dk is an alias for fatherdomain.dk"
        // "aliasdomain3.dk is an alias for fatherdomain3.dk"
        // and data should not contain strings: "aliasdomain1.dk is an alias for fatherdomain1.dk",
        //                                      "aliasdomain2.dk is an alias for fatherdomain2.dk"

        String expectedURL =
            String.format("metadata://netarkivet.dk/crawl/setup/aliases?majorversion=1&minorversion=0&harvestid=%s&harvestnum=%s&jobid=%s",
                    origHarvestDefinitionID, harvestNum, jobId);
        assertEquals("URL is not correctly written to the MetadataEntry",
                expectedURL, meta.getURL());
        assertEquals("Mimetype is not correctly written to the MetadataEntry",
                "text/plain", meta.getMimeType());
        StringAsserts.assertStringContains("Alias not correctly written to entry",
                "netarkivet.dk is an alias for kb.dk",
                new String(meta.getData()));
        StringAsserts.assertStringContains("Alias not correctly written to entry",
                "aliasdomain.dk is an alias for fatherdomain.dk",
                new String(meta.getData()));
        StringAsserts.assertStringContains("Alias not correctly written to entry",
                "aliasdomain3.dk is an alias for fatherdomain3.dk",
                new String(meta.getData()));
        StringAsserts.assertStringNotContains("Alias not correctly written to entry",
                "aliasdomain1.dk is an alias for fatherdomain1.dk",
                new String(meta.getData()));
        StringAsserts.assertStringNotContains("Alias not correctly written to entry",
                "aliasdomain2.dk is an alias for fatherdomain2.dk",
                new String(meta.getData()));
    }

    public void testGetExpirationDate() {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime()
                - Constants.ALIAS_TIMEOUT_IN_MILLISECONDS
                - 500L);

        AliasInfo expired = new AliasInfo("aliasdomain2.dk", "fatherdomain2.dk",
                expiredDate);
        assertEquals("Expired date should be in the past",
                new Date(now.getTime() - 500L),
                expired.getExpirationDate());

        AliasInfo nonExpired = new AliasInfo(aliasDomain, afatherDomain, now);
        assertEquals("Non-expired date should be in the future",
                new Date(now.getTime() + Constants.ALIAS_TIMEOUT_IN_MILLISECONDS),
                nonExpired.getExpirationDate());
    }
}
