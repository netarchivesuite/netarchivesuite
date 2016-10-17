package dk.netarkivet.harvester.webinterface.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

public class Heritrix3JobMonitorThread implements Runnable {

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(Heritrix3JobMonitorThread.class);

    protected static HarvestMonitor harvestMonitor;

    protected static JobDAO jobDAO;

    protected static RunningJobsInfoDAO runningJobsInfoDAO;

    static {
        harvestMonitor = HarvestMonitor.getInstance();
        jobDAO = JobDAO.getInstance();
        runningJobsInfoDAO = RunningJobsInfoDAO.getInstance();
    }

    public Thread thread;

    public boolean bExit = false;

    public Map<Long, Heritrix3JobMonitor> runningJobMonitorMap = new TreeMap<Long, Heritrix3JobMonitor>();

    public Map<Long, Heritrix3JobMonitor> filterJobMonitorMap = new TreeMap<Long, Heritrix3JobMonitor>();

    public void start() {
        thread = new Thread(this, "Heritrix3 Job Monitor Thread");
        thread.start();
    }

    @Override
    public void run() {
        Map<Long, Heritrix3JobMonitor> tmpJobMonitorMap;
        Iterator<Heritrix3JobMonitor> jobmonitorIter;
        byte[] tmpBuf = new byte[1024*1024];
        try {
            LOG.info("CrawlLog Thread started.");

            //File tmpFolder = new File("/tmp/");
            File tmpFolder = new File(".");
            File[] oldFiles = tmpFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith("crwawllog-")) {
                        if (name.endsWith(".log") || name.endsWith(".idx")) {
                            return true;
                        }
                    }
                    return false;
                }
            });

            Map<String, File> oldFilesMap = new HashMap<String, File>();
            File tmpFile;
            for (int i=0; i<oldFiles.length; ++i) {
                tmpFile = oldFiles[i];
                oldFilesMap.put(tmpFile.getName(), tmpFile);
            };
            List<File> oldFilesList = new ArrayList<File>();

            while (!bExit) {
                @SuppressWarnings("unchecked")
                Set<Long> runningJobs = harvestMonitor.getRunningJobs();
                Iterator<Long> jobidIter = runningJobs.iterator();
                Heritrix3JobMonitor jobmonitor;
                synchronized (runningJobMonitorMap) {
                    filterJobMonitorMap.clear();
                    while (jobidIter.hasNext()) {
                        long jobId = jobidIter.next();
                        jobmonitor = runningJobMonitorMap.remove(jobId);
                        if (jobmonitor == null) {
                            try {
                                jobmonitor = Heritrix3WrapperManager.getJobMonitor(jobId);
                            } catch (IOException e) {
                            }
                        }
                        filterJobMonitorMap.put(jobId, jobmonitor);
                    }
                    tmpJobMonitorMap = filterJobMonitorMap;
                    filterJobMonitorMap = runningJobMonitorMap;
                    runningJobMonitorMap = tmpJobMonitorMap;
                }
                jobmonitorIter = filterJobMonitorMap.values().iterator();
                while (jobmonitorIter.hasNext()) {
                    jobmonitor = jobmonitorIter.next();
                    oldFilesList.add(jobmonitor.logFile);
                    oldFilesList.add(jobmonitor.idxFile);
                    jobmonitor.dispose();
                }
                jobmonitorIter = runningJobMonitorMap.values().iterator();
                while (jobmonitorIter.hasNext()) {
                    jobmonitor = jobmonitorIter.next();
                    if (oldFilesMap != null) {
                        oldFilesMap.remove(jobmonitor.logFile.getName());
                        oldFilesMap.remove(jobmonitor.idxFile.getName());
                    }
                    jobmonitor.updateCrawlLog(tmpBuf);
                }
                if (oldFilesMap != null) {
                    oldFilesList.addAll(oldFilesMap.values());
                    oldFilesMap = null;
                }
                int idx = 0;
                while (idx < oldFilesList.size()) {
                    if (oldFilesList.get(idx).delete()) {
                        idx++;
                    } else {
                        oldFilesList.remove(idx);
                    }
                }
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                }
            }
            LOG.info("CrawlLog Thread stopped.");
        } catch (Throwable t) {
            LOG.error("CrawlLog Thread stopped unexpectedly!.", t);
        }
    }

    public Heritrix3JobMonitor getRunningH3Job(long jobId) {
        Heritrix3JobMonitor h3Job;
        synchronized (runningJobMonitorMap) {
            h3Job = runningJobMonitorMap.get(jobId);
        }
        return h3Job;
    }

    public List<Heritrix3JobMonitor> getRunningH3Jobs() {
        List<Heritrix3JobMonitor> h3JobsList = new LinkedList<Heritrix3JobMonitor>();
        synchronized (runningJobMonitorMap) {
            h3JobsList.addAll(runningJobMonitorMap.values());
        }
        return h3JobsList;
    }

}
