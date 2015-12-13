package dk.netarkivet.harvester.datamodel.eav;

import java.sql.Connection;
import java.sql.Timestamp;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;
import com.antiaction.raptor.sql.DBWrapper;

/**
 * Generic EAV attribute.
 */
public class ContentAttribute_Generic extends AttributeBase {

	/**
	 * Construct attribute using the attribute type to initialise common fields.
	 * @param at attribute type
	 */
	public ContentAttribute_Generic(AttributeTypeBase at) {
		attributeType = at;
		tree_id = at.tree_id;
		type_id = at.id;
	}

	@Override
	public void saveState(DBWrapper db, Connection conn) {
		if ( id == 0 ) {
			db.attribute_insert( conn, this );
		}
		else {
			db.attribute_update( conn, this );
		}
	}

	@Override
	public Integer getInteger() {
		if ( attributeType.datatype == 1 ) {
			return val_int;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void setInteger(Integer integer) {
		if ( attributeType.datatype == 1 ) {
			val_int = integer;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Timestamp getTimestamp() {
		if ( attributeType.datatype == 2 ) {
			return val_datetime;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void setTimestamp(Timestamp timestamp) {
		if ( attributeType.datatype == 2 ) {
			val_datetime = timestamp;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public String getVarchar() {
		if ( attributeType.datatype == 3 ) {
			return val_varchar;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void setVarchar(String varchar) {
		if ( attributeType.datatype == 3 ) {
			val_varchar = varchar;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public String getText() {
		if ( attributeType.datatype == 4 ) {
			return val_text;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void setText(String text) {
		if ( attributeType.datatype == 4 ) {
			val_text = text;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

}
