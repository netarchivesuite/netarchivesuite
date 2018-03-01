/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common;

import java.io.File;
import java.io.FileFilter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;

public class CleanupTester {

    private String[] dirsToClean = new String[] {"derbyDB/wayback_indexer_db", "oldjobs"};

    private File tmpdir;

    @Before
    public void setUp() throws Exception {
        tmpdir = FileUtils.getTempDir();
    }

    /**
     * Remove files in FileUtils.getTempDir();
     */
    @Ignore
    @Test
    public void testThatMostTmpFilesGone() {

        File[] files = tmpdir.listFiles(new SvnFileFilter());
        for (File f : files) {
            FileUtils.removeRecursively(f);
        }
        File tmp = new File("tmp");
        File tmp1 = new File("tmp1");
        FileUtils.remove(tmp);
        FileUtils.remove(tmp1);
        for (String fileToDelete : dirsToClean) {
            File f = new File(fileToDelete);
            System.out.println("Ready to delete file " + f.getAbsolutePath());
            FileUtils.removeRecursively(f);
        }
        // FIXME: Nothing is tested.
    }

    class SvnFileFilter implements FileFilter {
        public boolean accept(File f) {
            return !f.getName().equalsIgnoreCase(".svn");
        }
    }
}
