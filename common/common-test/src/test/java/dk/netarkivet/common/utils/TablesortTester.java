package dk.netarkivet.common.utils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.TableSort.SortOrder;
import junit.framework.TestCase;

/**
 * Unittests for the {@link TableSort} class. 
 */
public class TablesortTester extends TestCase {
 
    public TablesortTester(String s) {
        super(s);
    }
    /**
     * Test of TableSort constructor.
     * Note: no validation of columnId in TableSort class.
     */
    public void testConstructor() {
        try {
            new TableSort(0, null);
            fail("Should throw ArgumentNotValid on null SortOrder, but didn't");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        // this is currently valid, but probably shouldn't be
        try {
            new TableSort(-99, SortOrder.INCR);
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid on negative columnID, but did");
        }
        
        TableSort ts = new TableSort(0, TableSort.SortOrder.DESC);
        assertEquals(0, ts.getColumnIdent());
        assertEquals(TableSort.SortOrder.DESC, ts.getOrder());
    }
    
    public void testGetters() {
        TableSort ts = new TableSort(0, TableSort.SortOrder.DESC);
        ts.setColumnIdent(99);
        assertEquals(99, ts.getColumnIdent());
        assertEquals(TableSort.SortOrder.DESC, ts.getOrder());
        ts.setOrder(TableSort.SortOrder.INCR);
        assertEquals(99, ts.getColumnIdent());
        assertEquals(TableSort.SortOrder.INCR, ts.getOrder());
    }
    
}