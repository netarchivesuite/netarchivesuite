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

import java.util.List;

import org.hibernate.Session;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

/**
 * Data Access Object for ArchiveFile instances.
 */
@SuppressWarnings({"unchecked"})
public class ArchiveFileDAO extends GenericHibernateDAO<ArchiveFile, String> {

    /**
     * Default constructor.
     */
    public ArchiveFileDAO() {
        super(ArchiveFile.class);
    }

    /**
     * Returns true iff this file is found in the object store.
     *
     * @param filename the name of the file.
     * @return whether or not the file is already known.
     */
    public boolean exists(String filename) {
        Session sess = getSession();
        return !sess.createQuery("from ArchiveFile where filename='" + filename + "'").list().isEmpty();
    }

    /**
     * Returns a list of all files awaiting indexing, ie all files not yet indexed and which have not failed indexing
     * more than the maximum number of allowed times. The list is ordered such that previously failed files are returned
     * last.
     *
     * @return the list of files awaiting indexing.
     */
    public List<ArchiveFile> getFilesAwaitingIndexing() {
        int maxFailedAttempts = Settings.getInt(WaybackSettings.WAYBACK_INDEXER_MAXFAILEDATTEMPTS);
        return getSession().createQuery(
                "FROM ArchiveFile WHERE indexed=false" + " AND indexingFailedAttempts <  " + maxFailedAttempts
                        + " ORDER BY indexingFailedAttempts ASC").list();
    }

}
