package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.PermissionDenied;

public class MySQLSpecifics extends DBSpecifics {

    /** The log. */
    Log log = LogFactory.getLog(MySQLSpecifics.class);
    
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
        return "com.mysql.jdbc.Driver";
    }

}
