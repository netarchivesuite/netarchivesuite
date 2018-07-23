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

package dk.netarkivet.viewerproxy.webinterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;

/**
 * Batchjob that extracts lines from a crawl log matching a regular expression The batch job should be restricted to run
 * on metadata files for a specific job only, using the {@link #processOnlyFilesMatching(String)} construct.
 */
@SuppressWarnings({"serial"})
public class CrawlLogLinesMatchingRegexp extends ArchiveBatchJob {

    /** The logger. */
    //private final Log log = LogFactory.getLog(getClass().getName());
    private static final Logger log = LoggerFactory.getLogger(CrawlLogLinesMatchingRegexp.class);

    /** Metadata URL for crawl logs. */
    private static final String SETUP_URL_FORMAT = String.format("metadata://%s/crawl/logs/crawl.log",
            Settings.get(CommonSettings.ORGANIZATION));

    /** The regular expression to match in the crawl.log line. */
    private final String regexp;

    /**
     * Initialise the batch job.
     *
     * @param regexp The regexp to match in the crawl.log lines.
     */
    public CrawlLogLinesMatchingRegexp(String regexp) {
        ArgumentNotValid.checkNotNullOrEmpty(regexp, "regexp");
        this.regexp = regexp;

        /**
         * One week in milliseconds.
         */
        batchJobTimeout = 7 * Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Does nothing, no initialisation is needed.
     *
     * @param os Not used.
     */
    @Override
    public void initialize(OutputStream os) {
    }

    @Override
    public ArchiveBatchFilter getFilter() {
        return new ArchiveBatchFilter("OnlyCrawlLog") {
            public boolean accept(ArchiveRecordBase record) {
                String URL = record.getHeader().getUrl();
                if (URL == null) {
                    return false;
                } else {
                    return URL.startsWith(SETUP_URL_FORMAT);
                }
            }
        };
    }

    /**
     * Process a record on crawl log concerning the given domain to result.
     *
     * @param record The record to process.
     * @param os The output stream for the result.
     * @throws ArgumentNotValid on null parameters
     * @throws IOFailure on trouble processing the record.
     */
    @Override
    public void processRecord(ArchiveRecordBase record, OutputStream os) {
        ArgumentNotValid.checkNotNull(record, "ArchiveRecordBase record");
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        BufferedReader arcreader = new BufferedReader(new InputStreamReader(record.getInputStream()));
        try {
            for (String line = arcreader.readLine(); line != null; line = arcreader.readLine()) {
                if (line.matches(regexp)) {
                    os.write(line.getBytes("UTF-8"));
                    os.write('\n');
                }

            }
        } catch (IOException e) {
            throw new IOFailure("Unable to process (w)arc record", e);
        } finally {
            try {
                arcreader.close();
            } catch (IOException e) {
                log.warn("unable to close arcreader probably", e);
            }
        }
    }

    /**
     * Does nothing, no finishing is needed.
     *
     * @param os Not used.
     */
    @Override
    public void finish(OutputStream os) {
    }

    @Override
    public String toString() {
        return getClass().getName() + ", with arguments: Regexp = " + regexp + ", Filter = " + getFilter();
    }
}
