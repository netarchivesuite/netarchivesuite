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

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileNameHarvester {

    /**
     * Logger for this class.
     */
    private static Log log = LogFactory.getLog(FileNameHarvester.class);

    /**
     * This method harvests a list of all the files currently in the
     * arcrepository and appends any new ones found to the ArchiveFile
     * object store.
     * TODO there is a possible scaling issue because we get a list of
     * every file in the archive every time. We could profitably use a more
     * varied strategy where we retrieve all recent files several times a
     * day but run a "catch-all" job once in while to read every filename in
     * case we missed some due to machine outages.
     */
    public static synchronized void harvest() {
        ArchiveFileDAO dao = new ArchiveFileDAO();
        PreservationArcRepositoryClient client = ArcRepositoryClientFactory
                .getPreservationInstance();
        BatchStatus status = client.batch(new FileListJob(),
                                 Settings.get(WaybackSettings.WAYBACK_REPLICA));
        RemoteFile results = status.getResultFile();
        InputStream is = results.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null){
                if (!dao.exists(line.trim())) {
                    ArchiveFile file = new ArchiveFile();
                    file.setFilename(line.trim());
                    file.setIndexed(false);
                    log.info("Creating object store entry for '" +
                             file.getFilename() + "'");
                    dao.create(file);
                } // If the file is already known in the persistent store, no
                // action needs to be taken.
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading remote file", e);
        }
    }

}
