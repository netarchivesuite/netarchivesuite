package dk.netarkivet.harvester.webinterface.servlet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.netarchivesuite.heritrix3wrapper.AnypathResult;
import org.netarchivesuite.heritrix3wrapper.ByteRange;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.JobResult;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

public class Heritrix3JobMonitor implements Pageable {

    public boolean bActive = true;

    public boolean bInitialized;

    public long jobId;

    public Job job;

    public Heritrix3Wrapper h3wrapper;

    public String hostUrl;

    public String jobname;

    public JobResult jobResult;

    public String crawlLogFilePath;

    public File logFile;

    public RandomAccessFile logRaf;

    public File idxFile;

    public RandomAccessFile idxRaf;

    public long lastIndexed = 0;

    protected Heritrix3JobMonitor() {
    }

    public static Heritrix3JobMonitor getInstance(Long jobId) throws IOException {
        Heritrix3JobMonitor jobmonitor = new Heritrix3JobMonitor();
        jobmonitor.jobId = jobId;
        jobmonitor.logFile = new File("crawllog-" + jobId + ".log");
        jobmonitor.idxFile = new File("crawllog-" + jobId + ".idx");
        jobmonitor.init();
        return jobmonitor;
    }

    public synchronized void init() throws IOException {
        if (bActive && !bInitialized) {
            if (job == null) {
                job = Heritrix3JobMonitorThread.jobDAO.read(jobId);
            }
            if (h3wrapper == null) {
                StartedJobInfo startedInfo = Heritrix3JobMonitorThread.runningJobsInfoDAO.getMostRecentByJobId(jobId);
                if (startedInfo != null) {
                    hostUrl = startedInfo.getHostUrl();
                    if (hostUrl != null && hostUrl.length() > 0) {
                        h3wrapper = Heritrix3WrapperManager.getHeritrix3Wrapper(hostUrl);
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
                idxRaf.writeLong(0);
                bInitialized = true;
            }
        }
    }

    public synchronized void updateCrawlLog(byte[] tmpBuf) throws IOException {
        long pos;
        long to;
        int idx;
        boolean bLoop;
        ByteRange byteRange;
        if (bActive && !bInitialized) {
            init();
        }
        if (bActive && bInitialized) {
            bLoop = true;
            while (bLoop) {
                idxRaf.seek(idxRaf.length());
                pos = logRaf.length();
                to = pos;
                AnypathResult anypathResult = h3wrapper.anypath(jobResult.job.crawlLogFilePath, pos, pos + tmpBuf.length - 1);
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
            }
        }
    }

    @Override
    public synchronized long getIndexSize() {
        return idxFile.length();
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
            searchResult = new SearchResult(this, q, searchResultNr++);
            qSearchResultMap.put(q, searchResult);
        }
        return searchResult;
    }

    public synchronized void cleanup(List<File> oldFilesList) {
        bActive = false;
        bInitialized = false;
        hostUrl = null;
        h3wrapper = null;
        jobname = null;
        jobResult = null;
        crawlLogFilePath = null;
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
    }

}
