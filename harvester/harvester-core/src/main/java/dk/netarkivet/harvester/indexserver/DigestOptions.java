/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Encapsulates the options for the indexing process.
 */
public class DigestOptions {

    /** the useBlacklist set to true results in docs matching the mimefilter being ignored. */
    private final boolean useBlacklist;

    /**
     * An regular expression for the mimetypes to include or exclude from the index. According to the useBlacklist
     * setting.
     */
    private final String mimeFilter;

    /** Avoid logging to STDOUT when indexing. */
    private final boolean verbose;

    /**
     * Set the needed options used by the DigestIndexer.
     *
     * @param useMimefilterAsBlacklist Are we using the mimeFilter as a black or a whitelist.
     * @param verboseIndexing print logging to stdout while indexing, or not.
     * @param theMimeFilter The given black or whitelist according to mimetype.
     */
    public DigestOptions(boolean useMimefilterAsBlacklist, boolean verboseIndexing, String theMimeFilter) {
        ArgumentNotValid.checkNotNullOrEmpty(theMimeFilter, "String theMimeFilter");
        this.useBlacklist = useMimefilterAsBlacklist;
        this.mimeFilter = theMimeFilter;
        this.verbose = verboseIndexing;
    }

    /**
     * @return true if we use the mimefilter as a blacklist; false otherwise
     */
    public boolean getUseBlacklist() {
        return this.useBlacklist;
    }

    /**
     * @return true, if we are verbose; otherwise false
     */
    public boolean getVerboseMode() {
        return this.verbose;
    }

    /**
     * @return the mimefilter
     */
    public String getMimeFilter() {
        return this.mimeFilter;
    }

}
