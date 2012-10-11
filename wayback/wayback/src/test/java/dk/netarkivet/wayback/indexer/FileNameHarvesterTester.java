/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.util.List;

public class FileNameHarvesterTester extends IndexerTestCase {


    public void testHarvest() {
        FileNameHarvester.harvestAllFilenames();
        ArchiveFileDAO dao = new ArchiveFileDAO();
        List<ArchiveFile> files = dao.getSession().createQuery("from ArchiveFile").list();
        assertEquals("There should be four files", 6, files.size());
        FileNameHarvester.harvestAllFilenames();
        assertEquals("There should still be four files", 6, files.size());      
    }

}
