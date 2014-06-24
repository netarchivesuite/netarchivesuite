
package dk.netarkivet.harvester.datamodel.extendedfield;

import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValue;
import junit.framework.TestCase;

public class ExtendedFieldDefaultValuesTester extends TestCase {

    public ExtendedFieldDefaultValuesTester(String s) {
        super(s);
    }

    public void testInValid() {
    	ExtendedFieldDefaultValue e = null;
    	
    	e = new ExtendedFieldDefaultValue("", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("foo", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());

    	e = new ExtendedFieldDefaultValue("bar", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());

    	e = new ExtendedFieldDefaultValue(null, null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("a", "0000", ExtendedFieldDataTypes.NUMBER);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("12:08", "hh:mm a", ExtendedFieldDataTypes.TIMESTAMP);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("12:03", "dd/MM/yyyy", ExtendedFieldDataTypes.JSCALENDAR);
    	assertFalse(e.isValid());
    	
    }
    
    public void testValid() {
    	ExtendedFieldDefaultValue e = null;

    	e = new ExtendedFieldDefaultValue("true", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValue("t", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValue("1", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValue("false", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValue("f", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValue("true", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValue("0", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("0012", "0000", ExtendedFieldDataTypes.NUMBER);
    	assertTrue(e.isValid());

    	e = new ExtendedFieldDefaultValue("12:08 PM", "h:mm a", ExtendedFieldDataTypes.TIMESTAMP);
    	assertTrue(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("12:08", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
    	assertTrue(e.isValid());
    	
    	// this is also valid, because not the whole string will be used for parsing!
    	e = new ExtendedFieldDefaultValue("12:08:00", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
    	assertTrue(e.isValid());
    	
    	e = new ExtendedFieldDefaultValue("12/03/2000", "dd/MM/yyyy", ExtendedFieldDataTypes.JSCALENDAR);
    	assertTrue(e.isValid());
    }
    
    public void testValuesForDB() {
    	ExtendedFieldDefaultValue e = null;
    	String val;
    	
        e = new ExtendedFieldDefaultValue("0012", "0000", ExtendedFieldDataTypes.NUMBER);
        val = e.getDBValue();
        assertEquals(val, "12.0");
        
        e = new ExtendedFieldDefaultValue("00:01", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
        val = e.getDBValue();
        assertEquals(val, "60000");

        e = new ExtendedFieldDefaultValue("12/03/2000", "dd/MM/yyyy", ExtendedFieldDataTypes.JSCALENDAR);
        val = e.getDBValue();
        assertEquals(val, "952819200000");
        
        
    }
    
    
}
