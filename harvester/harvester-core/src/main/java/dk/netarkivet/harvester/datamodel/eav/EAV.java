package dk.netarkivet.harvester.datamodel.eav;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;
import com.antiaction.raptor.sql.DBWrapper;
import com.antiaction.raptor.sql.SqlResult;
import com.antiaction.raptor.sql.mssql.MSSql;

import dk.netarkivet.harvester.datamodel.HarvestDBConnection;

/**
 * EAV wrapper for the actual EAV implementation.
 */
public class EAV {
    
    private static final Logger log = LoggerFactory.getLogger(EAV.class);

	/** tree id for <code>SparseFullHarvest</code> attributes and types. */
	public static final int SNAPSHOT_TREE_ID = 1;
	/** tree id for <code>DomainConfiguration</code> attributes and types. */
	public static final int DOMAIN_TREE_ID = 2;

	/** Classloader used to instantiate attribute type implementations. */
	protected static ClassLoader classLoader = EAV.class.getClassLoader();

	/** Singleton instance. */
	protected static EAV instance;

	/**
     * @return an instance of this class.
     */
    public static synchronized EAV getInstance() {
        if (instance == null) {
            instance = new EAV();
        }
        return instance;
    }

    /** DB layer implementation. MSSQL is more like SQL92. */
    protected DBWrapper db = MSSql.getInstance(null, classLoader);

    /**
     * Insert attribute into database.
     * @param attribute attribute to insert into database
     */
    public void insertAttribute(AttributeBase attribute) {
        Connection connection = HarvestDBConnection.get();
        db.attribute_insert(connection, attribute);
        HarvestDBConnection.release(connection);
    }

    /**
     * Update existing attribute.
     * @param attribute attribute to update in database
     */
    public void saveAttribute(AttributeBase attribute) {
        Connection connection = HarvestDBConnection.get();
    	attribute.saveState(db, connection);
        HarvestDBConnection.release(connection);
    }

    /**
     * Returns a list of attribute types for the given tree id.
     * @param tree_id tree id to look for attribute types in
     * @return a list of attribute types for the given tree id
     */
    public List<AttributeTypeBase> getAttributeTypes(int tree_id) {
        Connection connection = HarvestDBConnection.get();
		List<AttributeTypeBase> attributeTypes = db.getAttributeTypes(connection, classLoader, tree_id);
        HarvestDBConnection.release(connection);
		return attributeTypes;
	}

    /**
     * Handy class to pair an attribute and its type.
     */
    public static class AttributeAndType implements Comparable<AttributeAndType> {
    	public AttributeTypeBase attributeType;
    	public AttributeBase attribute;
    	public AttributeAndType(AttributeTypeBase attributeType, AttributeBase attribute) {
        	this.attributeType = attributeType;
        	this.attribute = attribute;
    	}

		/**
		 * Comparator allows a list to be sorted so attributes appear in a reproducible order.
		 * @param o
		 * @return
		 */
		@Override public int compareTo(AttributeAndType o) {
			 return attributeType.id - o.attributeType.id;
		}
	}

    /**
     * Returns a list of attributes and their type for a given entity id and tree id.
     * @param tree_id tree id to look in
     * @param entity_id entity to look for
     * @return a list of attributes and their type for a given entity id and tree id.
     * @throws SQLException if an SQL exception occurs while querying the database
     */
    public List<AttributeAndType> getAttributesAndTypes(int tree_id, int entity_id) throws SQLException {
        Connection connection = HarvestDBConnection.get();
		List<AttributeTypeBase> attributeTypes = db.getAttributeTypes(connection, classLoader, tree_id);
		AttributeTypeBase attributeType;
		Map<Integer, AttributeTypeBase> attributeTypesMap = new TreeMap<Integer, AttributeTypeBase>();
		for (int i=0; i<attributeTypes.size(); ++i) {
			attributeType = attributeTypes.get(i);
			attributeTypesMap.put(attributeType.id, attributeType);
		}
        SqlResult sqlResult = db.attributes_getTypedAttributes(connection, tree_id, entity_id);
        List<AttributeAndType> attributes = new ArrayList<AttributeAndType>();
        AttributeBase attribute;
        ResultSet rs = sqlResult.rs;
		while ( rs.next() ) {
			int type_id = rs.getInt("type_id");
			attributeType = attributeTypesMap.get(type_id);
			attribute = null;
			rs.getInt("id");
			if (attributeType != null && !rs.wasNull()) {
				attribute = attributeType.instanceOf();
				attribute.attributeType = attributeType;
				attribute.loadState( db, connection, rs );
			}
			attributes.add(new AttributeAndType(attributeType, attribute));
		}
		sqlResult.close();
        HarvestDBConnection.release(connection);
		return attributes;
	}

	/**
  * Compare two lists containing attributes and their types.
  * @param antList1
  * @param antList2
  * @return the result of comparing two lists containing attributes and their types
  */
	public static int compare(List<AttributeAndType> antList1, List<AttributeAndType> antList2) {
		//For the vast majority of domains, we just have default attributes so let's deal
		//with that case efficiently right away:
	    
		if (antList1 == null && antList2 == null) {
			return 0;
		}
		if (antList1 == null) {
			antList1 = new ArrayList<>();
		}
		if (antList2 == null) {
			antList2 = new ArrayList<>();
		}
		if (antList1.isEmpty() && antList2.isEmpty()) {
			return 0;
		}
		int size1 = antList1.size();
		int size2 = antList2.size();
		if (!antList1.isEmpty() && !antList2.isEmpty() &&  size1 != size2) { // both non-empty but different length
			throw new UnsupportedOperationException("Haven't though about how to compare attribute lists of different lengths");
		}

		//Case where one list is empty, the other isn't. Check that all values are equal to default.
		if (antList1.isEmpty() || antList2.isEmpty()) {
		    boolean antList2Empty = antList2.isEmpty(); 
			List<AttributeAndType> antList3 = new ArrayList<AttributeAndType>();
			antList3.addAll(antList1); 
			antList3.addAll(antList2);
			for (AttributeAndType attributeAndType: antList3) {
				Integer defaultValue = attributeAndType.attributeType.def_int;
				
				Integer value = null;
				if (defaultValue == null) {
					defaultValue = 0;
				}
				
				if (attributeAndType.attribute != null) {
					value = attributeAndType.attribute.getInteger();
				}
				
				if (value == null) {
					value = defaultValue;
				}
				
				
				int res = value -defaultValue;
				
				if (res != 0L) { // value different from default
				    int result = res < 0L ? -1 : 1;
				    if (antList2Empty) {
				        result = -result;
				    }
					return result;
				}
			}
			return 0;
		}

		//Case where neither list is empty. Compare attribute by attribute.
		Collections.sort(antList1);
		Collections.sort(antList2);
		for (int i = 0; i < size1; i++) {
			AttributeAndType ant1 = antList1.get(i);
			AttributeAndType ant2 = antList2.get(i);
			if (ant1.attributeType.id != ant2.attributeType.id) {
				return (ant1.attributeType.id - ant2.attributeType.id) < 0 ? -1 : 1;
			}
			if (ant1.attributeType.datatype != 1) {
				throw new UnsupportedOperationException("EAV attribute datatype compare not implemented yet.");
			}
			Integer i1 = null;
			Integer i2 = null;
			if (ant1.attribute != null) {
				i1 = ant1.attribute.getInteger();
			}
			if (i1 == null) {
				i1 = ant1.attributeType.def_int;
			}
			if (i1 == null) {
				i1 = 0;
			}
			if (ant2.attribute != null) {
				i2 = ant2.attribute.getInteger();
			}
			if (i2 == null) {
				i2 = ant2.attributeType.def_int;
			}
			if (i2 == null) {
				i2 = 0;
			}
			int res = i2 - i1;
			if (res != 0L) {
				return res < 0L ? -1 : 1;
			}
		}
		return 0;
	}


    
    /////////////////// Utility methods //////////////////////////////////////
    /**
     * Get list of attribute names for a specific tree_id
     * @param tree_id a given tree_id
     * @return list of attribute names for a specific tree_id
     */
    public static Set<String> getAttributeNames(int tree_id) {
        EAV eav = EAV.getInstance();
        Set<String> attributeNames = new HashSet<String>();
        List<AttributeTypeBase> attributeTypes = eav.getAttributeTypes(tree_id);
        for (AttributeTypeBase atb: attributeTypes) {
           log.trace("Adding {} to list of attributenames", atb.name); 
           attributeNames.add(atb.name);
        }
        log.debug("The list of available attributeNames are {}", StringUtils.join(attributeNames, ","));
        return attributeNames;
    }

}
