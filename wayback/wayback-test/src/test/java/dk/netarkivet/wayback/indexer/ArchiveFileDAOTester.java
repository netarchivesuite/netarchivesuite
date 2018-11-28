/*
 * #%L
 * Netarchivesuite - wayback - test
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
package dk.netarkivet.wayback.indexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ArchiveFileDAOTester {

    @Before
    public void setUp() throws Exception {
        HibernateUtil.getSession().getSessionFactory().close();
    }

    @After
    public void tearDown() throws Exception {
        HibernateUtil.getSession().getSessionFactory().close();
    }

    @Test
    public void testCreateAndRead() {
        ArchiveFile file1 = new ArchiveFile();
        file1.setFilename("foobar");
        file1.setIndexed(false);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        String id = dao.create(file1);
        ArchiveFile file2 = dao.read(id);
        assertEquals("Filenames should be the same", file1.getFilename(), file2.getFilename());
        assertEquals("File statoi should be the same", file1.isIndexed(), file2.isIndexed());
    }

    @Test
    @Ignore("surefire fails:  GenericJDBC could not insert: [dk.netarki...")
    public void testExists() {
        ArchiveFile file1 = new ArchiveFile();
        file1.setFilename("foobar");
        file1.setIndexed(false);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        String id = dao.create(file1);
        assertTrue("File should exist", dao.exists("foobar"));
        assertFalse("File should not exist", dao.exists("barfoo"));
        assertTrue(id != null && !id.isEmpty());
    }

    @Test
    @Ignore("surefire fails: ConstraintViolation could not insert:...")
    public void testNotIndexed() {
        ArchiveFile file1 = new ArchiveFile();
        file1.setFilename("foobar");
        file1.setIndexed(false);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        String id1 = dao.create(file1);
        assertTrue(id1 != null && !id1.isEmpty());
        ArchiveFile file2 = new ArchiveFile();
        file2.setFilename("foobarbar");
        file2.setIndexed(false);
        file2.setIndexingFailedAttempts(1);
        String id2 = dao.create(file2);
        assertTrue(id2 != null && !id2.isEmpty());
        assertEquals("Should have 2 unindexed files", 2, dao.getFilesAwaitingIndexing().size());
        ArchiveFile file3 = new ArchiveFile();
        file3.setFilename("foofoobarbar");
        file3.setIndexed(false);
        file3.setIndexingFailedAttempts(100);
        String id3 = dao.create(file3);
        assertTrue(id3 != null && !id3.isEmpty());
        assertEquals("Should still have 2 unindexed files", 2, dao.getFilesAwaitingIndexing().size());
        assertEquals("Untried file should be returned first", file1.getFilename(), dao.getFilesAwaitingIndexing()
                .get(0).getFilename());
        file1.setIndexed(true);
        dao.update(file1);
        assertEquals("Should now have 1 unindexed file1", 1, dao.getFilesAwaitingIndexing().size());
    }

}
