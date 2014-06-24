package dk.netarkivet.archive.arcrepository;

import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import junit.framework.Assert;
import junit.framework.TestCase;

/** Tests of the ReplicaType enum class. */
public class ReplicaTypeTester extends TestCase {

    public void testFromOrdinal() {
        Assert.assertEquals(ReplicaType.NO_REPLICA_TYPE, ReplicaType.fromOrdinal(0));
        assertEquals(ReplicaType.BITARCHIVE, ReplicaType.fromOrdinal(1));
        assertEquals(ReplicaType.CHECKSUM, ReplicaType.fromOrdinal(2));
        try {
            ReplicaType.fromOrdinal(3);
            fail("Should throw ArgumentNotValid. Has ReplicaType been changed");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
     
    public void testFromSetting() {
        try {
            ReplicaType.fromSetting(null);
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        assertEquals(ReplicaType.BITARCHIVE, 
                ReplicaType.fromSetting(ReplicaType.BITARCHIVE_REPLICATYPE_AS_STRING));
        assertEquals(ReplicaType.CHECKSUM, 
                ReplicaType.fromSetting(ReplicaType.CHECKSUM_REPLICATYPE_AS_STRING));
        assertEquals(ReplicaType.NO_REPLICA_TYPE, 
                ReplicaType.fromSetting(""));
        assertEquals(ReplicaType.NO_REPLICA_TYPE, 
                ReplicaType.fromSetting("not yet introduced type"));
        
    }
}
