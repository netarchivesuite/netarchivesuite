
package dk.netarkivet.harvester.datamodel;


/**
 * Implementation of DB-specific functions for the server-based Derby.
 */
public class DerbyServerSpecifics extends DerbySpecifics {
    /**
     * Get an instance of the Server Derby specifics.
     * @return Instance of the Derby specifics implementation
     */
    public static DBSpecifics getInstance() {
        return new DerbyServerSpecifics();
    }

    /** Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.ClientDriver";
    }
}
