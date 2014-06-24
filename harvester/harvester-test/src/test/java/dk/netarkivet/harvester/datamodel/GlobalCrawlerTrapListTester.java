
package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 *  Unittests for the GlobalCrawlerTrapList class.
 */
public class GlobalCrawlerTrapListTester extends DataModelTestCase {
    public GlobalCrawlerTrapListTester(String s) {
        super(s);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
      super.tearDown();
    }

    /**
     * Tests that we can construct a trap list.
     */
    public void testConstructor() throws FileNotFoundException {
        GlobalCrawlerTrapList trapList = new GlobalCrawlerTrapList(
                new FileInputStream(new File(TestInfo.TOPDATADIR, 
                        TestInfo.CRAWLER_TRAPS_01)), "a_name",
                "A Description", true);
        assertEquals("Expected 5 traps in this list", 5, trapList.getTraps().size());
    }



    /**
     * Tests that the constructor throws expected exceptions on bad data
     */
    public void testConstructorFail() throws FileNotFoundException {

        try {
            new GlobalCrawlerTrapList(
                    new FileInputStream(new File(TestInfo.TOPDATADIR, 
                            TestInfo.CRAWLER_TRAPS_02)), "",
                    "A Description", true);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }


        try {
            new GlobalCrawlerTrapList(
                    null, "a_name",
                    "A Description", true);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }

    /**
     * Unit test for  https://sbforge.org/jira/browse/NAS-1793
     * @throws FileNotFoundException
     */
    public void testBadRegexp() throws FileNotFoundException {
        try {
            new GlobalCrawlerTrapList(
                    new FileInputStream(new File(TestInfo.TOPDATADIR, 
                            TestInfo.CRAWLER_TRAPS_03)), "a_name",
                    "A Description", true);
            fail("Should have thrown ArgumentNotValid on bad regexp");
        } catch (ArgumentNotValid e) {
            //expected
        }

    }
}
