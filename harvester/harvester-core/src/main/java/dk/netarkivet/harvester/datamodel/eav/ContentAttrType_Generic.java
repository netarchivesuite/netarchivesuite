package dk.netarkivet.harvester.datamodel.eav;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;
import com.antiaction.raptor.dao.SecurityEntityBase;

/**
 * Generic EAV attribute type.
 */
public class ContentAttrType_Generic extends AttributeTypeBase {

	@Override
	public AttributeBase instanceOf() {
		return new ContentAttribute_Generic(this);
	}

	@Override
	public boolean hasReadGrant(SecurityEntityBase user, int id) {
		return true;
	}

	@Override
	public boolean hasWriteGrant(SecurityEntityBase user, int id) {
		return true;
	}

}
