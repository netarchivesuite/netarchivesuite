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

import java.util.concurrent.Future;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Stores the state of a indexing task. */
public class IndexingState {

    /** The Id of the job being indexed. */
    private final Long jobIdentifier;
    /** The full path to the index. */
    private final String index;
    /** The result object for the indexing task. */
    private final Future<Boolean> resultObject;

    /**
     * Constructor for an IndexingState object.
     *
     * @param jobId The ID of the Job being indexing.
     * @param indexingpath The full path to the index.
     * @param result The result object for the indexing task
     */
    public IndexingState(Long jobId, String indexingpath, Future<Boolean> result) {
        ArgumentNotValid.checkNotNull(jobId, "Long jobId");
        ArgumentNotValid.checkNotNullOrEmpty(indexingpath, "String indexingpath");
        ArgumentNotValid.checkNotNull(result, "Future<Boolean> result");
        this.jobIdentifier = jobId;
        this.index = indexingpath;
        this.resultObject = result;
    }

    /**
     * @return the Id of the job being indexed.
     */
    public Long getJobIdentifier() {
        return jobIdentifier;
    }

    /**
     * @return the full path to the index generated.
     */
    public String getIndex() {
        return index;
    }

    /**
     * @return the result of the indexing process.
     */
    public Future<Boolean> getResultObject() {
        return resultObject;
    }

    public String toString() {
        return "IndexingState for JobID #" + jobIdentifier + ": (index = " + index + ", IndexingDone = "
                + resultObject.isDone() + ")";
    }

}
