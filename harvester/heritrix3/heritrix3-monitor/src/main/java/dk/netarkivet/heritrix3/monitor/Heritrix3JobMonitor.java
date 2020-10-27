/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
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

package dk.netarkivet.heritrix3.monitor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.netarchivesuite.heritrix3wrapper.ByteRange;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.JobResult;
import org.netarchivesuite.heritrix3wrapper.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

public class Heritrix3JobMonitor implements Pageable {

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(Heritrix3JobMonitorThread.class);

    protected NASEnvironment environment;

    public boolean bActive = true;

    public boolean bPull = false;

    public boolean bInitialized;

    public long jobId;

    public Job job;

    public Heritrix3Wrapper h3wrapper;

    public String h3HostnamePort;

    public String hostUrl;

    public String jobname;

    public JobResult jobResult;

    public String crawlLogFilePath;

    public File logFile;

    public RandomAccessFile logRaf;

    public File idxFile;

    public RandomAccessFile idxRaf;

    public long lastIndexed = 0;
    
    public long totalCachedLines = 0;

    protected Heritrix3JobMonitor() {
    }

    public static Heritrix3JobMonitor getInstance(Long jobId, NASEnvironment environment) throws IOException {
        Heritrix3JobMonitor jobmonitor = new Heritrix3JobMonitor();
        jobmonitor.environment = environment;
        jobmonitor.jobId = jobId;
        jobmonitor.logFile = new File(environment.tempPath, "crawllog-" + jobId + ".log");
        jobmonitor.idxFile = new File(environment.tempPath, "crawllog-" + jobId + ".idx");
        jobmonitor.init();
        return jobmonitor;
    }

    public synchronized void init() {
    	try {
            if (bActive && !bInitialized) {
                if (job == null) {
                    job = Heritrix3JobMonitorThread.jobDAO.read(jobId);
                }
                if (h3wrapper == null) {
                    StartedJobInfo startedInfo = Heritrix3JobMonitorThread.runningJobsInfoDAO.getMostRecentByJobId(jobId);
                    if (startedInfo != null) {
                        hostUrl = startedInfo.getHostUrl();
                        if (hostUrl != null && hostUrl.length() > 0) {
                            h3wrapper = Heritrix3WrapperManager.getHeritrix3Wrapper(hostUrl, environment.h3AdminName, environment.h3AdminPassword);
                        }
                    }
                }
                if (jobname == null && h3wrapper != null) {
                    jobname = Heritrix3WrapperManager.getJobname(h3wrapper, jobId);
                }
                if ((jobResult == null || jobResult.job == null) && jobname != null) {
                    jobResult = h3wrapper.job(jobname);
                }
                if (jobResult != null && jobResult.job != null) {
                    crawlLogFilePath = jobResult.job.crawlLogFilePath;
                }
                if (crawlLogFilePath != null) {
                    logRaf = new RandomAccessFile(logFile, "rw");
                    idxRaf = new RandomAccessFile(idxFile, "rw");
                    if (idxRaf.length() == 0) {
                        idxRaf.writeLong(0);
                    } else {
                        idxRaf.seek(idxRaf.length() - 8);
                        lastIndexed = idxRaf.readLong();
                    	totalCachedLines = (idxRaf.length() / 8) - 1;
                    }
                    idxRaf.seek(idxRaf.length());
                    logRaf.seek(logRaf.length());
                    bInitialized = true;
                }
            }
    	} catch (Throwable t) {
    	}
    }

    public synchronized void update() {
    	try {
            if (job != null) {
                Job tmpJob = job = Heritrix3JobMonitorThread.jobDAO.read(jobId);
                if (tmpJob != null) {
                    job = tmpJob;
                }
            }
            if (jobResult != null && jobResult.job != null && jobname != null) {
                JobResult tmpJobResult = h3wrapper.job(jobname);
                if (tmpJobResult != null) {
                    jobResult = tmpJobResult;
                }
            }
    	} catch (Throwable t) {
    	}
    }

    public synchronized void updateCrawlLog(byte[] tmpBuf) {
        long pos;
        long to;
        int idx;
        boolean bLoop;
        ByteRange byteRange;
        try {
            if (bActive && !bInitialized) {
                init();
            }
            if (bActive && bInitialized) {
                bLoop = true;
                while (bLoop) {
                    idxRaf.seek(idxRaf.length());
                    pos = logRaf.length();
                    to = pos;
                    if (jobResult != null && jobResult.job != null && jobResult.job.crawlLogFilePath != null) {
                    	long rangeFrom = pos;
                    	long rangeTo = pos + tmpBuf.length - 1;
                        StreamResult anypathResult = h3wrapper.anypath(jobResult.job.crawlLogFilePath, null, null, true);
                        if (anypathResult != null && rangeFrom < anypathResult.contentLength) {
                            LOG.info("Crawllog length for job {}={}.", jobId, anypathResult.contentLength);
                        	if (rangeTo >= anypathResult.contentLength) {
                        		rangeTo = anypathResult.contentLength - 1;
                        	}
                        	anypathResult = h3wrapper.anypath(jobResult.job.crawlLogFilePath, rangeFrom, rangeTo);
                            LOG.info("Crawllog byterange download for job {}. ({}-{})", jobId, rangeFrom, rangeTo);
                            if (anypathResult != null && anypathResult.byteRange != null && anypathResult.in != null) {
                                byteRange = anypathResult.byteRange;
                                if (byteRange.contentLength > 0) {
                                    logRaf.seek(pos);
                                    int read;
                                    try {
                                        while ((read = anypathResult.in.read(tmpBuf)) != -1) {
                                            logRaf.write(tmpBuf, 0, read);
                                            to += read;
                                            idx = 0;
                                            while (read > 0) {
                                                ++pos;
                                                --read;
                                                if (tmpBuf[idx++] == '\n') {
                                                    idxRaf.writeLong(pos);
                                                    lastIndexed = pos;
                                                    totalCachedLines++;
                                                }
                                            }
                                        }
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    IOUtils.closeQuietly(anypathResult);
                                    if (byteRange.contentLength == to) {
                                        bLoop = false;
                                    }
                                } else {
                                    bLoop = false;
                                }
                            } else {
                                bLoop = false;
                            }
                        } else {
                            bLoop = false;
                        }
                    } else {
                        bLoop = false;
                    }
                }
            }
        } catch (Throwable t) {
        }
    }

    public synchronized void cleanup(List<File> oldFilesList) {
    	try {
            bActive = false;
            bInitialized = false;
            hostUrl = null;
            h3wrapper = null;
            jobname = null;
            jobResult = null;
            crawlLogFilePath = null;
            totalCachedLines = 0;
            IOUtils.closeQuietly(logRaf);
            IOUtils.closeQuietly(idxRaf);
            oldFilesList.add(logFile);
            oldFilesList.add(idxFile);
            Iterator<SearchResult> srIter = qSearchResultMap.values().iterator();
            SearchResult sr;
            while (srIter.hasNext()) {
                sr = srIter.next();
                oldFilesList.add(sr.srIdxFile);
                oldFilesList.add(sr.srLogFile);
                sr.cleanup();
            }
            qSearchResultMap.clear();
    	} catch (Throwable t) {
    	}
    }

    @Override
    public synchronized long getIndexSize() {
        return idxFile.length();
    }

    @Override
    public long getLastIndexed() {
        return lastIndexed;
    }
    
    public long getTotalCachedLines() {
    	return totalCachedLines;
    }

    @Override
    public synchronized byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException {
        return StringIndexFile.readPage(idxRaf, logRaf, page, itemsPerPage, descending);
    }

    public synchronized boolean isReady() {
        return (bActive && bInitialized);
    }

    protected Map<String, SearchResult> qSearchResultMap = new HashMap<String, SearchResult>();

    protected int searchResultNr = 1;

    public synchronized SearchResult getSearchResult(String q) throws IOException {
        SearchResult searchResult = qSearchResultMap.get(q);
        if (searchResult == null) {
            searchResult = new SearchResult(environment, this, q, searchResultNr++);
            qSearchResultMap.put(q, searchResult);
        }
        return searchResult;
    }

    /**
     * Set the file path to the crawl log
     *
     * @param crawlLogFilePath File path to the crawl log
     */
    public void setCrawlLogFilePath(String crawlLogFilePath) {
        this.crawlLogFilePath = crawlLogFilePath;
    }
}
