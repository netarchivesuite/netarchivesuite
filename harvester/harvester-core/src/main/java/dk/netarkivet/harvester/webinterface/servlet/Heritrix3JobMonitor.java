package dk.netarkivet.harvester.webinterface.servlet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.io.IOUtils;
import org.netarchivesuite.heritrix3wrapper.AnypathResult;
import org.netarchivesuite.heritrix3wrapper.ByteRange;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.JobResult;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

public class Heritrix3JobMonitor {

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
        jobmonitor.logFile = new File("crwawllog-" + jobId + ".log");
        jobmonitor.idxFile = new File("crwawllog-" + jobId + ".idx");
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

    public synchronized boolean isReady() {
        return (bActive && bInitialized);
    }

    public synchronized byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException {
        byte[] bytes = null;;
        if (page < 1) {
            throw new IllegalArgumentException();
        }
        if (itemsPerPage < 25) {
            throw new IllegalArgumentException();
        }
        long length = idxRaf.length();
        if (length > 8) {
            if (!descending) {
                // Forwards.
                long fromIdx = (page - 1) * (itemsPerPage * 8);
                long toIdx = fromIdx + (itemsPerPage * 8);
                if (toIdx > length) {
                    toIdx = length;
                }
                idxRaf.seek(fromIdx);
                fromIdx = idxRaf.readLong();
                idxRaf.seek(toIdx);
                toIdx = idxRaf.readLong();
                logRaf.seek(fromIdx);
                bytes = new byte[(int)(toIdx - fromIdx)];
                logRaf.readFully(bytes, 0, (int)(toIdx - fromIdx));
            } else {
                // Backwards.
                long toIdx = length - ((page - 1) * itemsPerPage * 8);
                long fromIdx = toIdx - (itemsPerPage * 8) - 8;
                if (fromIdx < 0) {
                    fromIdx = 0;
                }
                // Read line indexes for page.
                int pageIdxArrLen = (int)(toIdx - fromIdx);
                byte[] pageIdxArr = new byte[pageIdxArrLen];
                idxRaf.seek(fromIdx);
                int pos = 0;
                int limit = pageIdxArrLen;
                int read = 0;
                while (limit > 0 && read != -1) {
                    read = idxRaf.read(pageIdxArr, pos, limit);
                    if (read != -1) {
                        pos += read;
                        limit -= read;
                    }
                }
                // Convert line indexes for page.
                limit = pos;
                pos = 0;
                long[] idxArr = new long[limit / 8];
                long l;
                int dstIdx = 0;
                while (pos < limit) {
                    l = (pageIdxArr[pos++] & 255) << 56 | (pageIdxArr[pos++] & 255) << 48 | (pageIdxArr[pos++] & 255) << 40 | (pageIdxArr[pos++] & 255) << 32
                            | (pageIdxArr[pos++] & 255) << 24 | (pageIdxArr[pos++] & 255) << 16 | (pageIdxArr[pos++] & 255) << 8 | (pageIdxArr[pos++] & 255);
                    idxArr[dstIdx++] = l;
                }
                // Load the crawllog lines for page.
                pos = 0;
                limit /= 8;
                fromIdx = idxArr[pos];
                toIdx = idxArr[limit - 1];
                logRaf.seek(fromIdx);
                byte[] tmpBytes = new byte[(int)(toIdx - fromIdx)];
                logRaf.readFully(tmpBytes, 0, (int)(toIdx - fromIdx));
                // Reverse crawllog lines for page.
                bytes = new byte[tmpBytes.length];
                long base = idxArr[pos++];
                fromIdx = base;
                int len;
                dstIdx = bytes.length;
                while (pos < limit) {
                    toIdx = idxArr[pos++];
                    len = (int)(toIdx - fromIdx);
                    dstIdx -= len;
                    System.arraycopy(tmpBytes, (int)(fromIdx - base), bytes, dstIdx, len);
                    fromIdx = toIdx;
                }
            }
        }
        return bytes;
    }

    public void search() {
    }

    public synchronized void dispose() {
        bActive = false;
        bInitialized = false;
        hostUrl = null;
        h3wrapper = null;
        jobname = null;
        jobResult = null;
        crawlLogFilePath = null;
        IOUtils.closeQuietly(logRaf);
        IOUtils.closeQuietly(idxRaf);
    }

}
