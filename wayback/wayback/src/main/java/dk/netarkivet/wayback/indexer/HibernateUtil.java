/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback.indexer;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * This class contains a single static utility method which returns a Hibernate
 * session: HibernateUtil.getSession().
 *
 * The configuration for the session is read from settings.xml in the elements
 * nested under settings/wayback/hibernate.
 */
public class HibernateUtil {

    /** Logger for this class. */
    private static final Log log = LogFactory.getLog(
            HibernateUtil.class.getName());

    /** Key indicating the connection provider to be used by hibernate.
     */
    private static final String CONNECTION_PROVIDER_CLASS
            = "connection.provider_class";

    /**
     * Value indicating use of the c3p0 as connection provider. This is hard
     * coded and no other connection providers have been tested.
     */
    private static final String
            ORG_HIBERNATE_CONNECTION_C3_P0_CONNECTION_PROVIDER
            = "org.hibernate.connection.C3P0ConnectionProvider";

    // For documentation of these values see the corresponding constants in
    // the WaybackSettings class
    private static final String C3P0_ACQUIRE_INCREMENT
            = "c3p0.acquire_increment";
    private static final String C3P0_IDLE_TEST_PERIOD = "c3p0.idle_test_period";
    private static final String C3P0_MAX_SIZE = "c3p0.max_size";
    private static final String C3P0_MAX_STATEMENTS = "c3p0.max_statements";
    private static final String C3P0_MIN_SIZE = "c3p0.min_size";
    private static final String C3P0_TIMEOUT = "c3p0.timeout";
    private static final String HIBERNATE_CONNECTION_DRIVER_CLASS
            = "hibernate.connection.driver_class";
    private static final String HIBERNATE_CONNECTION_URL
            = "hibernate.connection.url";
    private static final String HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String HIBERNATE_FORMAT_SQL = "hibernate.format_sql";
    private static final String HIBERNATE_BYTECODE_USE_REFLECTION_OPTIMIZER
            = "hibernate.bytecode.use_reflection_optimizer";
    private static final String HIBERNATE_HBM2DDL_AUTO
            = "hibernate.hbm2ddl.auto";
    private static final String HIBERNATE_TRANSACTION_FACTORY_CLASS
            = "hibernate.transaction.factory_class";
    private static final String HIBERNATE_SHOW_SQL = "hibernate.show_sql";
    private static final String HIBERNATE_CONNECTION_USERNAME
            = "hibernate.connection.username";
    private static final String HIBERNATE_CONNECTION_PASSWORD
            = "hibernate.connection.password";

    /**
     * Private constructor as this class is never instantiated.
     */
    private HibernateUtil() {}

    /**
     * The session factory from which sessions are obtained. There is no
     * accessor supplied for this factory as a reference to it can be obtained
     * if necessary (e.g. during a cleanup operation) from the Session itself. 
     */
    private static SessionFactory sessionFactory;

    /**
     * Properties of the hibernate session are loaded from settings.xml. There
     * is therefore no need for a separate hibernate configuration file.
     */
    private static void initialiseFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {
                AnnotationConfiguration config = new AnnotationConfiguration();
                config.setProperty(CONNECTION_PROVIDER_CLASS,
                                   ORG_HIBERNATE_CONNECTION_C3_P0_CONNECTION_PROVIDER);
                config.setProperty(C3P0_ACQUIRE_INCREMENT,
                         Settings.get(WaybackSettings.C3P0_ACQUIRE_INCREMENT));
                config.setProperty(C3P0_IDLE_TEST_PERIOD,
                               Settings.get(WaybackSettings.C3P0_IDLE_PERIOD));
                config.setProperty(C3P0_MAX_SIZE,
                                   Settings.get(WaybackSettings.C3P0_MAX_SIZE));
                config.setProperty(C3P0_MAX_STATEMENTS,
                             Settings.get(WaybackSettings.C3P0_MAX_STATEMENTS));
                config.setProperty(C3P0_MIN_SIZE,
                                   Settings.get(WaybackSettings.C3P0_MIN_SIZE));
                config.setProperty(C3P0_TIMEOUT,
                                   Settings.get(WaybackSettings.C3P0_TIMEOUT));
                config.setProperty(HIBERNATE_CONNECTION_DRIVER_CLASS,
                             Settings.get(WaybackSettings.HIBERNATE_DB_DRIVER));
                config.setProperty(HIBERNATE_CONNECTION_URL,
                                Settings.get(WaybackSettings.HIBERNATE_DB_URL));
                config.setProperty(HIBERNATE_DIALECT,
                               Settings.get(WaybackSettings.HIBERNATE_DIALECT));
                config.setProperty(HIBERNATE_FORMAT_SQL,
                            Settings.get(WaybackSettings.HIBERNATE_FORMAT_SQL));
                config.setProperty(HIBERNATE_BYTECODE_USE_REFLECTION_OPTIMIZER,
                  Settings.get(WaybackSettings.HIBERNATE_REFLECTION_OPTIMIZER));
                config.setProperty(HIBERNATE_HBM2DDL_AUTO,
                          Settings.get(WaybackSettings.HIBERNATE_HBM2DDL_AUTO));
                config.setProperty(HIBERNATE_TRANSACTION_FACTORY_CLASS,
                   Settings.get(WaybackSettings.HIBERNATE_TRANSACTION_FACTORY));
                config.setProperty(HIBERNATE_SHOW_SQL,
                              Settings.get(WaybackSettings.HIBERNATE_SHOW_SQL));
                // Specifically allow unset username/password for the database
                // so that we can use database without authentication, e.g. in
                // testing.
                if (!Settings.get(WaybackSettings.HIBERNATE_USERNAME).isEmpty()) {
                    config.setProperty(HIBERNATE_CONNECTION_USERNAME,
                              Settings.get(WaybackSettings.HIBERNATE_USERNAME));
                }
                if (!Settings.get(WaybackSettings.HIBERNATE_PASSWORD).isEmpty()) {
                    config.setProperty(HIBERNATE_CONNECTION_PASSWORD,
                              Settings.get(WaybackSettings.HIBERNATE_PASSWORD));
                }
                config.addAnnotatedClass(ArchiveFile.class);
                sessionFactory = config.buildSessionFactory();
            } catch (Throwable ex) {
                log.fatal("Could not connect to hibernate object store - "
                          + "exiting", ex);
                throw new IllegalStateException("Could not connect to hibernate "
                                                + "object store - exiting", ex);
            }
        }
    }

    /**
     * Get a hibernate session for communicating with the object store for
     * the wayback indexer. This method has the side effect of creating and
     * initialising a SessionFactory object if there is no current open
     * SessionFactory.
     * @return the abovementioned hibernate session.
     */
    public static Session getSession() {
        initialiseFactory();
        return sessionFactory.openSession();
    }

}
