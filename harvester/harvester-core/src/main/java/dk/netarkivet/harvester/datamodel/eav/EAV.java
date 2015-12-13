package dk.netarkivet.harvester.datamodel.eav;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

	public static final int SNAPSHOT_TREE_ID = 1;
	public static final int DOMAIN_TREE_ID = 2;

	protected static ClassLoader classLoader = EAV.class.getClassLoader();

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

    protected DBWrapper db = MSSql.getInstance(null, classLoader);

    public void insertAttribute(AttributeBase attribute) {
        Connection connection = HarvestDBConnection.get();
        db.attribute_insert(connection, attribute);
        HarvestDBConnection.release(connection);
    }

    public void saveAttribute(AttributeBase attribute) {
        Connection connection = HarvestDBConnection.get();
    	attribute.saveState(db, connection);
        HarvestDBConnection.release(connection);
    }

    public List<AttributeTypeBase> getAttributeTypes(int tree_id) {
        Connection connection = HarvestDBConnection.get();
		List<AttributeTypeBase> attributeTypes = db.getAttributeTypes(connection, classLoader, tree_id);
        HarvestDBConnection.release(connection);
		return attributeTypes;
	}

    public static class AttributeAndType {
    	public AttributeTypeBase attributeType;
    	public AttributeBase attribute;
    	public AttributeAndType(AttributeTypeBase attributeType, AttributeBase attribute) {
        	this.attributeType = attributeType;
        	this.attribute = attribute;
    	}
    }

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

}
