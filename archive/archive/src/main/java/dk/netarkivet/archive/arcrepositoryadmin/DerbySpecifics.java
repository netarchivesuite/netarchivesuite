
package dk.netarkivet.archive.arcrepositoryadmin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Derby-specific implementation of DB methods.
 *
 * This class is intended for potential updates of the database, if it sometime
 * in the future needs to be updated.
 * Since it is the first version of the database, no update method have yet been
 * implemented.
 */
public abstract class DerbySpecifics extends DBSpecifics {
    /** The log.*/
    protected Log log = LogFactory.getLog(DerbySpecifics.class);
}
