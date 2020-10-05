/*
 * #%L
 * Netarchivesuite - wayback
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to enable retry of indexing for selected files after they have reached maxFailedAttempts.
 */
public class ResetFailedFiles {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(ResetFailedFiles.class);

    /**
     * Usage: java -cp dk.netarkivet.wayback.jar
     * -Ddk.netarkivet.settings.file=/home/test/TEST12/conf/settings_WaybackIndexerApplication.xml
     * -Dsettings.common.applicationInstanceId=RESET_FILES dk.netarkivet.wayback.indexer.ResetFailedFiles file1 file2
     * ...
     * <p>
     * The given files are reset so that they appear never to have failed an indexing attempt. They will therefore be
     * placed in the index queue the next time the indexer runs.
     *
     * @param args the file names
     */
    public static void main(String[] args) {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        for (String filename : args) {
            ArchiveFile archiveFile = dao.read(filename);
            if (archiveFile != null) {
                log.info("Resetting to 0 failures for '{}'", archiveFile.getFilename());
                archiveFile.setIndexingFailedAttempts(0);
                dao.update(archiveFile);
            } else {
                log.warn("Attempt to process unknown file '{}'", filename);
            }
        }
    }

}
