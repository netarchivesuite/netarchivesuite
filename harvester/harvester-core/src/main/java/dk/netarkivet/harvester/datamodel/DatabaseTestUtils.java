/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.datamodel;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * Utilities to allow testing databases. 
 * FIXME: Rename without Test as these are not specifically test related.
 */
public class DatabaseTestUtils {

    protected static final Logger log = LoggerFactory.getLogger(DatabaseTestUtils.class);

    private static String dburi;

    /**
     * Get access to the database stored in the given file. This will start a new transaction that will be rolled back
     * with dropDatabase. Only one connection can be taken at a time.
     *
     * @param resourcePath A file that contains a test database.
     * @param dbCreationDir
     * 
     */
    public static void createDatabase(String resourcePath, String dbname, File dbCreationDir) throws Exception {
        Settings.set(CommonSettings.DB_MACHINE, "");
        Settings.set(CommonSettings.DB_PORT, "");
        Settings.set(CommonSettings.DB_DIR, "");

        FileUtils.removeRecursively(new File(dbCreationDir, dbname));

        final String dbfile = dbCreationDir + "/" + dbname;

        // FIXME: change for h2
        dburi = "jdbc:derby:" + dbfile;

        long startTime = System.currentTimeMillis();

        try (Connection c = DriverManager.getConnection(dburi + ";create=true");){
            c.setAutoCommit(false); // Do not commit individual .
            // locate create script first, next to resource
            File createFile = new File(new File(resourcePath).getParentFile(), "create.sql");
            applyStatementsInInputStream(c, checkNotNull(new FileInputStream(createFile), "create.sql"));

            // then populate it.
            FileInputStream is = checkNotNull(new FileInputStream(resourcePath), resourcePath);
            applyStatementsInInputStream(c, is);

            c.commit();
        }

        log.debug("Populated {} in {}(ms)", dbfile, (System.currentTimeMillis() - startTime));
    }

    private static void applyStatementsInInputStream(Connection connection, InputStream is) throws SQLException,
            IOException {
        Statement statement = connection.createStatement();

        LineNumberReader br = new LineNumberReader(new InputStreamReader(is));
        String s = "";
        long count = 0;
        try {
            while ((s = br.readLine()) != null) {
                log.debug(br.getLineNumber() + ": " + s);
                if (s.trim().startsWith("#")) {
                    // skip comments
                } else if (s.trim().length() == 0) {
                    // skip empty lines
                } else {
                    count++;
                    {
                        // http://apache-database.10148.n7.nabble.com/Inserting-control-characters-in-SQL-td106944.html
                        s = s.replace("\\n", "\n");
                    }
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
     * Get access to the database stored in the given file. This will start a new transaction that will be rolled back
     * with dropDatabase. Only one connection can be taken at a time.
     *
     * @param resourcePath A file that contains a test database.
     * @param dbCreationDir
     */
    public static void createDatabase(String resourcePath, File dbCreationDir) throws Exception {
        createDatabase(resourcePath, "derivenamefromresource", dbCreationDir);
    }

    /**
     * Get a connection to the given sample harvest definition database and fool the HD DB connect class into thinking
     * it should use that one.
     *
     * @param resourcePath Location of the sql files to create and populate the test DB.
     * @param dbCreationDir
     */
    public static void createHDDB(String resourcePath, String dbname, File dbCreationDir) throws Exception {
        createDatabase(resourcePath, dbname, dbCreationDir);
    }

    /**
     * Drop access to the database that's currently taken.
     */
    public static void dropDatabase() throws Exception {
        try {
            final String shutdownUri = dburi + ";shutdown=true";
            DriverManager.getConnection(shutdownUri);
            throw new IOFailure("Failed to shut down database");
        } catch (SQLException e) {
            log.warn("Expected SQL-exception when shutting down database:", e);
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
         * for (Thread t: connectionPool.keySet()) { final Connection connection = connectionPool.get(t); if
         * (!(connection instanceof TestDBConnection)) { throw new UnknownID("Illegal connection " + connection); } try
         * { if (savepoints.containsKey(t)) { connection.rollback(); // connection.rollback(savepoints.get(t));
         * savepoints.remove(t); } } catch (SQLException e) { System.out.println("Can't rollback: " + e); }
         * connection.close(); } connectionPool.clear();
         */
    }

    /**
     * Drop the connection to the harvest definition database.
     */
    public static void dropHDDB() throws Exception {
        dropDatabase();
        log.debug("dropHDDB() 1");
        HarvestDBConnection.cleanup();
    }
}
