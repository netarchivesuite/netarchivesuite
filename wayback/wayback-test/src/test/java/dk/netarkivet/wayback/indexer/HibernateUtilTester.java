/*
 * #%L
 * Netarchivesuite - wayback - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.wayback.indexer;

import junit.framework.TestCase;
import org.hibernate.Session;

public class HibernateUtilTester extends TestCase {

    /**
     * Tests that we can create an open session.
     */
    public void testGetSession() {
        Session session = HibernateUtil.getSession();
        assertTrue("Session should be connected.", session.isConnected());
        assertTrue("Session should be open.", session.isOpen());
        assertFalse("Session should not be dirty.", session.isDirty());
    }

}
