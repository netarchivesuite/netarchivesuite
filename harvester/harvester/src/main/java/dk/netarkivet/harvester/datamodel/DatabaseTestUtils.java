/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

package dk.netarkivet.harvester.datamodel;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.ZipUtils;

//import dk.netarkivet.testutils.ReflectUtils;

/**
 * Utilities to allow testing databases. //FIXME: Rename without Test as these
 * are not specifically test related.
 */
public class DatabaseTestUtils {

    private static String dburi;
    protected static final Logger log = Logger.getLogger(DatabaseTestUtils.class.getName());

    /**
     * Get access to the database stored in the given file. This will start a
     * new transaction that will be rolled back with dropDatabase. Only one
     * connection can be taken at a time.
     * 
     * @param resourcePath
     *            A file that contains a test database.
     * @param dbCreationDir
     * @return a connection to the database stored in the given file
     * @throws SQLException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static Connection takeDatabase(String resourcePath, String dbname, File dbCreationDir) throws SQLException,
            IOException, IllegalAccessException {

        Settings.set(CommonSettings.DB_MACHINE, "");
        Settings.set(CommonSettings.DB_PORT, "");
        Settings.set(CommonSettings.DB_DIR, "");

        FileUtils.removeRecursively(new File(dbCreationDir, dbname));

        final String dbfile = dbCreationDir + "/" + dbname;

        // FIXME: change for h2
        dburi = "jdbc:derby:" + dbfile;

        System.err.println("Populating " + dbfile + " from " + resourcePath);
        Connection c = DriverManager.getConnection(dburi + ";create=true");
        applyStatementsInInputStream(c, DatabaseTestUtils.class.getResourceAsStream("/create-hddb.sql"));

        // then populate it.
        FileInputStream is = new FileInputStream(resourcePath);
        applyStatementsInInputStream(c, is);

        System.err.println("Populated...");
        //
        c.close();

        return DriverManager.getConnection(dburi);
    }

    @SuppressWarnings("unused")
    private static void applyStatementsInInputStream(Connection connection, InputStream is) throws SQLException,
            IOException {
        // if (resourceName.startsWith(LEGACY_FILE_PREFIX_FOR_TEST_RESOURCES)) {
        // resourceName =
        // resourceName.substring(LEGACY_FILE_PREFIX_FOR_TEST_RESOURCES.length());
        // }
        Statement statement = connection.createStatement();
        // InputStream is = new FileInputStream(resourceName);
        // if (is == null) {
        // throw new IOException("Resource not found: " + resourceName);
        // }
        LineNumberReader br = new LineNumberReader(new InputStreamReader(is));
        String s = "";
        long count = 0;
        try {
            while ((s = br.readLine()) != null) {
                log.info(br.getLineNumber() + ": " + s);
                if (s.trim().startsWith("#")) {
                    // skip comments
                } else if (s.trim().length() == 0) {
                    // skip empty lines
                } else {
                    count++;
                    statement.execute(s);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Line " + br.getLineNumber() + ": " + s, e);
        }
        br.close();
        statement.close();
        if (count == 0) {
            throw new RuntimeException("Executed " + count + " SQL commands.");
        }
    }

    /**
     * Get access to the database stored in the given file. This will start a
     * new transaction that will be rolled back with dropDatabase. Only one
     * connection can be taken at a time.
     * 
     * @param resourcePath
     *            A file that contains a test database.
     * @param dbCreationDir
     * @return a connection to the database stored in the given file
     * @throws SQLException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static Connection takeDatabase(String resourcePath, File dbCreationDir) throws SQLException, IOException,
            IllegalAccessException {
        return takeDatabase(resourcePath, "derivenamefromresourcePath", dbCreationDir);
    }

    /**
     * Get a connection to the given sample harvest definition database and fool
     * the HD DB connect class into thinking it should use that one.
     * 
     * @param samplefile
     *            a sample harvest definition database
     * @param dbCreationDir
     * @return a connection to the given sample harvest definition database
     * @throws SQLException
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static Connection getHDDB(String resourcePath, String dbname, File dbCreationDir) throws SQLException,
            IOException,
            IllegalAccessException {
        return takeDatabase(resourcePath, dbname, dbCreationDir);
    }

    /**
     * Drop access to the database that's currently taken.
     * 
     * @throws SQLException
     */
    public static void dropDatabase() throws SQLException, NoSuchFieldException, IllegalAccessException {
        try {
            final String shutdownUri = dburi + ";shutdown=true";
            DriverManager.getConnection(shutdownUri);
            throw new IOFailure("Failed to shut down database");
        } catch (SQLException e) {
            log.warning("Expected SQL-exception when shutting down database:" + e);
        }
        // connectionPool.clear();
        // null field instance in DBSpecifics.

        // inlined to break test dependency /tra 2014-05-19
        // Field f = ReflectUtils.getPrivateField(DBSpecifics.class,
        // "instance");
        Field f = DBSpecifics.class.getDeclaredField("instance");
        f.setAccessible(true);

        f.set(null, null);
        /*
         * for (Thread t: connectionPool.keySet()) { final Connection connection
         * = connectionPool.get(t); if (!(connection instanceof
         * TestDBConnection)) { throw new UnknownID("Illegal connection " +
         * connection); } try { if (savepoints.containsKey(t)) {
         * connection.rollback(); // connection.rollback(savepoints.get(t));
         * savepoints.remove(t); } } catch (SQLException e) {
         * System.out.println("Can't rollback: " + e); } connection.close(); }
         * connectionPool.clear();
         */
    }

    /**
     * Drop the connection to the harvest definition database.
     * 
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws Exception
     */
    public static void dropHDDB() throws SQLException, NoSuchFieldException, IllegalAccessException {
        dropDatabase();
        log.info("dropHDDB() 1");
        HarvestDBConnection.cleanup();
    }
}
