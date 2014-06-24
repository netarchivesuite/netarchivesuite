
package dk.netarkivet.harvester.datamodel;


/**
 * A class that implement functionality specific to the embedded Derby system.
 */
public class DerbyEmbeddedSpecifics extends DerbySpecifics {
    /**
     * Get an instance of the Embedded Derby specifics.
     * @return Instance of the Derby specifics implementation
     */
    public static DBSpecifics getInstance() {
        return new DerbyEmbeddedSpecifics();
    }

    /** Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }


}
