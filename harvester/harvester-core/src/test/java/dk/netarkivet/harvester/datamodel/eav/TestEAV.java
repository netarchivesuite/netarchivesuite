package dk.netarkivet.harvester.datamodel.eav;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.antiaction.raptor.dao.AttributeTypeBase;

import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;

public class TestEAV {

    @Test
    public void testEAV() {
    	AttributeAndType aat;
    	AttributeTypeBase at;

    	List<AttributeAndType> antList1 = new ArrayList<AttributeAndType>();
    	List<AttributeAndType> antList2 = new ArrayList<AttributeAndType>();

    	Assert.assertEquals(0, EAV.compare(antList1, antList2));

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 1;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(1);
    	antList2.add(aat);
    	
  
    	Assert.assertEquals(1, EAV.compare(antList1, antList2)); 
   
    	Assert.assertEquals(-1, EAV.compare(antList2, antList1));

    	
    	antList1.add(aat);
         

    	Assert.assertEquals(0, EAV.compare(antList1, antList2));
    	Assert.assertEquals(0, EAV.compare(antList2, antList1));

    	antList1.clear();
    	antList2.clear();

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 1;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(1);
    	antList1.add(aat);

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 2;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(1);
    	antList2.add(aat);


    	Assert.assertNotEquals(0, EAV.compare(antList1, antList2));
    	Assert.assertEquals(-1*EAV.compare(antList1, antList2), EAV.compare(antList2, antList1));

    	antList1.clear();
    	antList2.clear();

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 1;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(1);
    	antList1.add(aat);
    	antList2.add(aat);

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 2;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(1);
    	antList1.add(aat);
		//Insert the attributes in antList2 in a different order - should still be identical
    	antList2.add(0, aat);


    	Assert.assertEquals(0, EAV.compare(antList1, antList2));
    	Assert.assertEquals(0, EAV.compare(antList2, antList1));

    	antList1.clear();
    	antList2.clear();

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 1;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(1);
    	antList1.add(aat);

    	at = new ContentAttrType_Generic();
    	at.tree_id = 1;
    	at.id = 1;
    	at.datatype = 1;
    	aat = new AttributeAndType(at, new ContentAttribute_Generic(at));
    	aat.attribute.setInteger(2);
    	antList2.add(aat);

    	Assert.assertEquals(1, EAV.compare(antList1, antList2));
    	Assert.assertEquals(-1, EAV.compare(antList2, antList1));
    }

}
