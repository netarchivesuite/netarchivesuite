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
    private final Log log = LogFactory.getLog(getClass().getName());

    private HibernateUtil() {}

    private static SessionFactory sessionFactory;

    private static void initialiseFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {

                    //dk.statsbiblioteket.digitaltv.utils.PropertiesUtil props = new dk.statsbiblioteket.digitaltv.utils.PropertiesUtil();
                    //String cfg = props.getProperty(props.DEFAULT_BUNDLE, PropertyNames.HIBERNATE_CFG);
                    //sessionFactory = new AnnotationConfiguration().configure(new File(cfg)).buildSessionFactory();
                    throw new NotImplementedException("Method not implemented");
            } catch (Throwable ex) {
                //System.err.println("Initial SessionFactory creation failed." + ex);
                //ex.printStackTrace();
                //throw new ExceptionInInitializerError(ex);
               throw new NotImplementedException("Method not implemented");
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
