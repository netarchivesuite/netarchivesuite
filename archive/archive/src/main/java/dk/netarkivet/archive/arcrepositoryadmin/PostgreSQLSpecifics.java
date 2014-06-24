package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.PermissionDenied;

public class PostgreSQLSpecifics extends DBSpecifics {

    /**
     * Factory method discoverable by reflection from
     * SettingsFactory.getInstance().
     * @return a new instance of theis class.
     */
    public static DBSpecifics getInstance() {
        return new PostgreSQLSpecifics();
    }

    /** The log. */
    Log log = LogFactory.getLog(PostgreSQLSpecifics.class);
    
    @Override
    public void shutdownDatabase() {
        log.warn("Attempt to shutdown the database ignored. Only meaningful "
                + "for embedded databases");
    }

    @Override
    public void backupDatabase(Connection c, File backupDir)
            throws SQLException, PermissionDenied {
        log.warn("Attempt to backup the database ignored. Only meaningful "
                + "for embedded databases");
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

}
