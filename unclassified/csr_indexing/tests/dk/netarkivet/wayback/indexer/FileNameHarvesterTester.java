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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import dk.netarkivet.archive.arcrepository.bitpreservation.FileListJob;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.TestInfo;

import junit.framework.TestCase;

public class FileNameHarvesterTester extends IndexerTestCase {


    public void testHarvest() {
        FileNameHarvester.harvest();
        ArchiveFileDAO dao = new ArchiveFileDAO();
        List<ArchiveFile> files = dao.getSession().createQuery("from ArchiveFile").list();
        assertEquals("There should be two files", 2, files.size());
        FileNameHarvester.harvest();
        assertEquals("There should still be two files", 2, files.size());      
    }

}
