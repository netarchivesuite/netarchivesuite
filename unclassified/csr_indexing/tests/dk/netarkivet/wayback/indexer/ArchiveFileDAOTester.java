/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import junit.framework.TestCase;

public class ArchiveFileDAOTester extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        HibernateUtil.getSession().getSessionFactory().close();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateUtil.getSession().getSessionFactory().close();
    }

    public void testCreateAndRead() {
        ArchiveFile file1 = new ArchiveFile();
        file1.setFilename("foobar");
        file1.setIndexed(false);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        String id = dao.create(file1);
        ArchiveFile file2 = dao.read(id);
        assertEquals("Filenames should be the same", file1.getFilename(),
                     file2.getFilename());
        assertEquals("File statoi should be the same", file1.isIndexed(),
                     file2.isIndexed());
    }

}
