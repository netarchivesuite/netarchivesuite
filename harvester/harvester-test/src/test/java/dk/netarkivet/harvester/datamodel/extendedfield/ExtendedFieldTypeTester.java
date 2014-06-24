package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.List;

import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldType;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypeDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypeDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;


public class ExtendedFieldTypeTester extends DataModelTestCase {
    public ExtendedFieldTypeTester(String aTestName) {
        super(aTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testRead() {
        ExtendedFieldTypeDAO extDAO = ExtendedFieldTypeDBDAO.getInstance();
        ExtendedFieldType type = null;

        type = extDAO.read(Long.valueOf(ExtendedFieldTypes.DOMAIN));
        assertEquals(type.getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.DOMAIN]);

        type = extDAO.read(Long.valueOf(ExtendedFieldTypes.HARVESTDEFINITION));
        assertEquals(
                type.getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.HARVESTDEFINITION]);

        ExtendedFieldTypeDAO extDAO2 = ExtendedFieldTypeDBDAO.getInstance();
        List<ExtendedFieldType> list = extDAO2.getAll();

        assertEquals(list.size(), 2);
        assertEquals(list.get(0).getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.DOMAIN]);
        assertEquals(
                list.get(1).getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.HARVESTDEFINITION]);
    }
}
