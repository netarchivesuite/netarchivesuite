
package dk.netarkivet.harvester.datamodel.extendedfield;

import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDBDAO;
import dk.netarkivet.harvester.webinterface.ExtendedFieldConstants;

public class ExtendedFieldValueTester  extends DataModelTestCase {

    public ExtendedFieldValueTester(String aTestName) {
        super(aTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateReadUpdateDelete() {
        ExtendedFieldDAO extDAO = ExtendedFieldDBDAO.getInstance();
        ExtendedField extField = new ExtendedField(null, 
                (long)ExtendedFieldTypes.DOMAIN, "Test", "12345", 1, true, 1, 
                "a", "b", ExtendedFieldConstants.MAXLEN_EXTF_NAME);
        extDAO.create(extField);

        ExtendedFieldValueDAO extValDAO = ExtendedFieldValueDBDAO.getInstance();
        
        ExtendedFieldValue extFieldVal = new ExtendedFieldValue(null, 
                extField.getExtendedFieldID(), Long.valueOf(100L), "foo");
        extValDAO.create(extFieldVal);

        ExtendedFieldValueDAO extValDAO2 = ExtendedFieldValueDBDAO.getInstance();
        extFieldVal = extValDAO2.read(1L, 100L);
        
        assertEquals(extFieldVal.getExtendedFieldValueID().longValue(), 1);
        assertEquals(extFieldVal.getExtendedFieldID().longValue(), 1);
        assertEquals(extFieldVal.getInstanceID(), Long.valueOf(100));
        assertEquals(extFieldVal.getContent(), "foo");
        
        extFieldVal.setContent("bar");
        
        ExtendedFieldValueDAO extValDAO3 = ExtendedFieldValueDBDAO.getInstance();
        extValDAO3.update(extFieldVal);

        assertEquals(extFieldVal.getExtendedFieldValueID().longValue(), 1);
        assertEquals(extFieldVal.getExtendedFieldID().longValue(), 1);
        assertEquals(extFieldVal.getInstanceID(), Long.valueOf(100));
        assertEquals(extFieldVal.getContent(), "bar");
    }
}
