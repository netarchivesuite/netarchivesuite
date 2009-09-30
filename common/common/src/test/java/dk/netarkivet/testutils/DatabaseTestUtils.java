/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.testutils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.harvester.datamodel.DBConnect;
import dk.netarkivet.harvester.datamodel.DBSpecifics;

/**
 * Utilities to allow testing databases.
 *
 */
public class DatabaseTestUtils {
    private static Map<Thread,Connection> connectionPool;
    private static String dburi;
    protected static final Logger log = 
        Logger.getLogger(DatabaseTestUtils.class.getName());


    /** Get access to the database stored in the given file.  This will start
     * a new transaction that will be rolled back with dropDatabase.
     * Only one connection can be taken at a time.
     *
     * @param jarfile A file that contains a test database.
     * @param dbUnzipDir
     * @return a connection to the database stored in the given file
     * @throws SQLException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static Connection takeDatabase(File jarfile, String dbname, File dbUnzipDir)
            throws SQLException, IOException, IllegalAccessException
    {
        //String dbname = jarfile.getName().substring(0, jarfile.getName().lastIndexOf('.'));

        FileUtils.removeRecursively(new File(dbUnzipDir, dbname));
        ZipUtils.unzip(jarfile, dbUnzipDir);
        // Absolute or relative path should work according to
        // http://incubator.apache.org/derby/docs/ref/rrefjdbc37352.html

        final String dbfile = dbUnzipDir + "/" + dbname;
        try {
            Field f = DBConnect.class.getDeclaredField("connectionPool");
            f.setAccessible(true);
            connectionPool = (WeakHashMap<Thread,Connection>) f.get(null);
        } catch (NoSuchFieldException e) {
            throw new PermissionDenied("Can't get connectionPool field", e);
        }
        // Make sure we're using the right DB in DBConnect

        /* Set DB name */
        try {
            String driverName = "org.apache.derby.jdbc.EmbeddedDriver";
            Class.forName(driverName).newInstance();
        } catch (Exception e) {
            throw new IOFailure("Can't register driver", e);
        }
        dburi = "jdbc:derby:" + dbfile;
        return DriverManager.getConnection(dburi);
            /*
            Field f = DBConnect.class.getDeclaredField("dbname");
            f.setAccessible(true);
            f.set(null, Settings.get(Settings.HARVESTDEFINITION_BASEDIR) + "/fullhddb"
                    + ";restoreFrom=" + new File(extractDir, dbname).getAbsolutePath());
            Method m = DBConnect.class.getDeclaredMethod("getDB", new Class[0]);
            m.setAccessible(true);
            return (Connection)m.invoke(null);
            */
    }

    /** Get a connection to the given sample harvest definition database
     * and fool the HD DB connect class into thinking it should use that one.
     * @param samplefile a sample harvest definition database
     * @param dbUnzipDir
     * @return a connection to the given sample harvest definition database
     * @throws SQLException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static Connection getHDDB(File samplefile, String dbname, File dbUnzipDir)
            throws SQLException, IOException, IllegalAccessException {
        return takeDatabase(samplefile, dbname, dbUnzipDir);
    }

    /** Drop access to the database that's currently taken.
     * @throws SQLException
     */
    public static void dropDatabase() throws SQLException,
    										NoSuchFieldException,
                                            IllegalAccessException{
        try {
            final String shutdownUri = dburi + ";shutdown=true";
            DriverManager.getConnection(shutdownUri);
            throw new IOFailure("Failed to shut down database");
        } catch (SQLException e) {
            log.warning(
                    "Expected SQL-exception when shutting down database:" + e);
        }
        connectionPool.clear();
        // null field instance in DBSpecifics.
        Field f = ReflectUtils.getPrivateField(DBSpecifics.class, "instance");
        f.set(null, null);
/*
        for (Thread t: connectionPool.keySet()) {
            final Connection connection = connectionPool.get(t);
            if (!(connection instanceof TestDBConnection)) {
                throw new UnknownID("Illegal connection " + connection);
            }
            try {
                if (savepoints.containsKey(t)) {
                    connection.rollback();
                   // connection.rollback(savepoints.get(t));
                    savepoints.remove(t);
                }
            } catch (SQLException e) {
                System.out.println("Can't rollback: " + e);
            }
            connection.close();
        }
        connectionPool.clear();
        */
    }


    /** Drop the connection to the harvest definition database.
     * @throws IllegalAccessException 
     * @throws NoSuchFieldException 
     * @throws Exception
     */
    public static void dropHDDB() throws SQLException,
                            NoSuchFieldException, IllegalAccessException {
        dropDatabase();
    }
}
