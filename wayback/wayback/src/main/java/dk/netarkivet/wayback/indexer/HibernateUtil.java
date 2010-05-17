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

import java.io.File;

import dk.netarkivet.common.exceptions.NotImplementedException;
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
    private static final Log log = LogFactory.getLog(HibernateUtil.class.getName());

    private HibernateUtil() {}

    private static SessionFactory sessionFactory;

    private static void initialiseFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {
                AnnotationConfiguration config = new AnnotationConfiguration();
                config.setProperty("connection.provider_class",
                            "org.hibernate.connection.C3P0ConnectionProvider");
                config.setProperty("c3p0.acquire_increment", Settings.get(WaybackSettings.C3P0_ACQUIRE_INCREMENT));
                config.setProperty("c3p0.idle_test_period", Settings.get(WaybackSettings.C3P0_IDLE_PERIOD));
                config.setProperty("c3p0.max_size", Settings.get(WaybackSettings.C3P0_MAX_SIZE));
                config.setProperty("c3p0.max_statements", Settings.get(WaybackSettings.C3P0_MAX_STATEMENTS));
                config.setProperty("c3p0.min_size", Settings.get(WaybackSettings.C3P0_MIN_SIZE));
                config.setProperty("c3p0.timeout", Settings.get(WaybackSettings.C3P0_TIMEOUT));
                config.setProperty("hibernate.connection.driver_class", Settings.get(WaybackSettings.HIBERNATE_DB_DRIVER));
                config.setProperty("hibernate.connection.url", Settings.get(WaybackSettings.HIBERNATE_DB_URL));
                config.setProperty("hibernate.dialect", Settings.get(WaybackSettings.HIBERNATE_DIALECT));
                config.setProperty("hibernate.format_sql", Settings.get(WaybackSettings.HIBERNATE_FORMAT_SQL));
                config.setProperty("hibernate.bytecode.use_reflection_optimizer", Settings.get(WaybackSettings.HIBERNATE_REFLECTION_OPTIMIZER));
                config.setProperty("hibernate.hbm2ddl.auto", Settings.get(WaybackSettings.HIBERNATE_HBM2DDL_AUTO));
                config.setProperty("hibernate.transaction.factory_class", Settings.get(WaybackSettings.HIBERNATE_TRANSACTION_FACTORY));
                config.setProperty("hibernate.show_sql", Settings.get(WaybackSettings.HIBERNATE_SHOW_SQL));
                if (!Settings.get(WaybackSettings.HIBERNATE_USERNAME).isEmpty()) {
                    config.setProperty("hibernate.connection.username", Settings.get(WaybackSettings.HIBERNATE_USERNAME));
                }
                if (!Settings.get(WaybackSettings.HIBERNATE_PASSWORD).isEmpty()) {
                    config.setProperty("hibernate.connection.password", Settings.get(WaybackSettings.HIBERNATE_PASSWORD));
                }
                config.addAnnotatedClass(ArchiveFile.class);
                sessionFactory = config.buildSessionFactory();
            } catch (Throwable ex) {
                log.fatal("Could not connect to hibernate object store - exiting", ex);
                throw new IllegalStateException("Could not connect to hibernate object store - exiting", ex);
            }
        }
    }

    private static SessionFactory getSessionFactory() {
        initialiseFactory();
        return sessionFactory;
    }

    public static Session getSession() {
        initialiseFactory();
        return sessionFactory.openSession();
    }

}
