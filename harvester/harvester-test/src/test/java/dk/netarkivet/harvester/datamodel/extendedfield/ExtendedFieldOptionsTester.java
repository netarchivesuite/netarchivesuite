
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.Map;

import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldOptions;

import junit.framework.TestCase;

public class ExtendedFieldOptionsTester extends TestCase {

    public ExtendedFieldOptionsTester(String s) {
        super(s);
    }

    public void testOptions() {
    	String line = null;
    	ExtendedFieldOptions eo = null;
    	Map<String, String> result = null;
    	
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());
    	
    	line = "";
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());

    	line = "\n";
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());

    	line = "\n\n";
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());
    	
    	line = "key";
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());

    	line = "key=";
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());

    	line = "=value";
    	eo = new ExtendedFieldOptions(line);
    	assertFalse(eo.isValid());

    	line = "key=value";
    	eo = new ExtendedFieldOptions(line);
    	assertTrue(eo.isValid());

    	line = "key=value";
    	eo = new ExtendedFieldOptions(line);
    	assertTrue(eo.isValid());
    	result = eo.getOptions();
    	assertEquals(result.get("key"), "value");

    	line = "key=value\nfoo=bar";
    	eo = new ExtendedFieldOptions(line);
    	assertTrue(eo.isValid());
    	result = eo.getOptions();
    	assertEquals(result.get("key"), "value");
    	assertEquals(result.get("foo"), "bar");
    	assertTrue(eo.isKeyValid("key"));
    	assertTrue(eo.isKeyValid("foo"));
    	assertFalse(eo.isKeyValid("bar"));

    	line = "key=value\nfoo=";
    	eo = new ExtendedFieldOptions(line);
    	assertTrue(eo.isValid());
    	result = eo.getOptions();
    	assertEquals(result.get("key"), "value");
    	assertEquals(result.size(), 1);
    	assertEquals(eo.getOptionsString(), "key" + ExtendedFieldOptions.KEYVALUESEPARATOR + "value" + ExtendedFieldOptions.NEWLINE);
    	assertTrue(eo.isKeyValid("key"));
    	assertFalse(eo.isKeyValid("foo"));
    }
}
