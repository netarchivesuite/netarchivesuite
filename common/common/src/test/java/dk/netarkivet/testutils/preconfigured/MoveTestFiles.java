/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.testutils.preconfigured;

import java.io.File;
import java.io.IOException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

public class MoveTestFiles implements TestConfigurationIF {
    private File originalsDir;
    private File workingDir;
    public MoveTestFiles(File originalsDir, File workingDir) {
        this.originalsDir = originalsDir;
        this.workingDir = workingDir;
    }
    public void setUp() {
        FileUtils.removeRecursively(workingDir);
        workingDir.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(originalsDir,workingDir);
    }

    public void tearDown() {
        setReadWrite(workingDir);
        FileUtils.removeRecursively(workingDir);
    }

    /**
     * Recursively set all files readable and writable. Used to ensure working
     * dir is deletable.
     * @param file File or directory to start from.
     */
    private void setReadWrite(File file) {
        file.setReadable(true);
        file.setWritable(true);
        if (file.isDirectory()) {
            for(File file1 : file.listFiles()) {
                setReadWrite(file1);
            }
        }
    }

    public File working(File f) {
        if (!f.getAbsolutePath().startsWith(originalsDir.getAbsolutePath())) {
            throw new ArgumentNotValid(f + " is not in " + originalsDir);
        }
        return new File(workingDir,f.getAbsolutePath().substring(originalsDir.getAbsolutePath().length()));
    }

    public File newTmpFile() {
        try {
            return File.createTempFile(this.getClass().getSimpleName(),"Tmp",workingDir);
        } catch (IOException e) {
            throw new IOFailure("Failed to create a temp file in " + workingDir,e);
        }
    }

    public File newTmpDir() {
        try {
            File tmpFile = File.createTempFile(this.getClass().getSimpleName(),"Tmp",workingDir);
            tmpFile.delete(); // Maybe not necessary
            tmpFile.mkdir();
            return tmpFile;
        } catch (IOException e) {
            throw new IOFailure("Failed to create a temp dir in " + workingDir,e);
        }
    }
}
