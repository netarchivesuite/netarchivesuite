package dk.netarkivet.common.utils.cdx;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Performs unit tests of the ARCFilenameCDXRecordFilter. Implicitly tests both
 * SimpleCDXRecordFilter and CDXRecordFilter
 */
public class ARCFilenameCDXRecordFilterTester extends TestCase {

    public void testConstructor() {
        try {
            new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter1");
        } catch (Exception e) {
            fail("Constuctor should not throw exception !");
        }
    }

    public void testGetFiltername() {
        SimpleCDXRecordFilter cdxfil = new ARCFilenameCDXRecordFilter(
                "NETARKIVET_00001.*", "filter1");
        assertEquals("Filtername are not the same !", cdxfil.getFilterName(),
                     "filter1");
    }

    public void testNullFiltername() {
        try {
            new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", null);
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

    public void testEmptyFiltername() {
        try {
            new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "");
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

    public void testEmptyFilenamePattern() {
        try {
            new ARCFilenameCDXRecordFilter("", "filter1");
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

    public void testNullFilenamePattern() {
        try {
            new ARCFilenameCDXRecordFilter(null, "filter1");
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

}
