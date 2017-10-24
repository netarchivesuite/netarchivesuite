package dk.netarkivet.harvester.webinterface.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

public class Heritrix3JobMonitorThread implements Runnable {

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(Heritrix3JobMonitorThread.class);

    protected NASEnvironment environment;

    protected static HarvestMonitor harvestMonitor;

    protected static JobDAO jobDAO;

    protected static RunningJobsInfoDAO runningJobsInfoDAO;

    protected static HarvestChannelDAO harvestChannelDAO;

    static {
        harvestMonitor = HarvestMonitor.getInstance();
        jobDAO = JobDAO.getInstance();
        runningJobsInfoDAO = RunningJobsInfoDAO.getInstance();
        harvestChannelDAO = HarvestChannelDAO.getInstance();
    }

    public Thread thread;

    public Throwable throwable;

    public boolean bExit = false;

    public Map<Long, Heritrix3JobMonitor> runningJobMonitorMap = new TreeMap<Long, Heritrix3JobMonitor>();

    public Map<Long, Heritrix3JobMonitor> filterJobMonitorMap = new TreeMap<Long, Heritrix3JobMonitor>();

    public Set<Heritrix3Wrapper> h3WrapperSet = new HashSet<Heritrix3Wrapper>();

    public Set<String> h3HostPortSet = new HashSet<String>();

    public List<String> h3HostnamePortEnabledList = new ArrayList<String>();

    public List<String> h3HostnamePortDisabledList = new ArrayList<String>();

    public Heritrix3JobMonitorThread(NASEnvironment environment) {
        this.environment = environment;
    }

    public synchronized void start() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this, "Heritrix3 Job Monitor Thread");
            thread.start();
        }
    }

    @Override
    public void run() {
        Map<Long, Heritrix3JobMonitor> tmpJobMonitorMap;
        Iterator<Heritrix3JobMonitor> jobmonitorIter;
        byte[] tmpBuf = new byte[1024 * 1024];
        try {
            LOG.info("Heritrix3 Job Monitor Thread started.");

            //File tmpFolder = new File("/tmp/");
            File tmpFolder = environment.tempPath;;
            File[] oldFiles = tmpFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith("crawllog-")) {
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
                Set<Long> runningJobs = getRunningJobs();
                if (runningJobs != null) {
                    Iterator<Long> jobidIter = runningJobs.iterator();
                    Heritrix3JobMonitor jobmonitor;
                    synchronized (runningJobMonitorMap) {
                        filterJobMonitorMap.clear();
                        while (jobidIter.hasNext()) {
                            Long jobId = jobidIter.next();
                            if (jobId != null) {
                                jobmonitor = runningJobMonitorMap.remove(jobId);
                                if (jobmonitor == null) {
                                    try {
                                        // New H3 job.
                                        jobmonitor = Heritrix3WrapperManager.getJobMonitor(jobId, environment);
                                    } catch (IOException e) {
                                    }
                                }
                                filterJobMonitorMap.put(jobId, jobmonitor);
                            }
                        }
                        tmpJobMonitorMap = filterJobMonitorMap;
                        filterJobMonitorMap = runningJobMonitorMap;
                        runningJobMonitorMap = tmpJobMonitorMap;
                        LOG.debug("runningJobMonitorMap.hashCode() from Heritrix3JobMonitorThread.run()",
                                runningJobMonitorMap.hashCode());
                    }
                    jobmonitorIter = filterJobMonitorMap.values().iterator();
                    while (jobmonitorIter.hasNext()) {
                        jobmonitor = jobmonitorIter.next();
                        jobmonitor.cleanup(oldFilesList);
                    }
                    jobmonitorIter = runningJobMonitorMap.values().iterator();
                    while (jobmonitorIter.hasNext()) {
                        jobmonitor = jobmonitorIter.next();
                        if (oldFilesMap != null) {
                            oldFilesMap.remove(jobmonitor.logFile.getName());
                            oldFilesMap.remove(jobmonitor.idxFile.getName());
                        }
                        if (!jobmonitor.bInitialized) {
                            jobmonitor.init();
                        }
                        checkH3HostnamePort(jobmonitor);
                        isH3HostnamePortEnabled(jobmonitor);
                        if (jobmonitor.bPull) {
                            jobmonitor.updateCrawlLog(tmpBuf);
                        }
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
                }
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                }
            }
            LOG.info("Heritrix3 Job Monitor Thread stopped.");
        } catch (Throwable t) {
            // Save throwable so we can show it in the restart GUI.
            throwable = t;
            LOG.error("Heritrix3 Job Monitor Thread stopped unexpectedly!.", t);
        }
    }

    /**
     * Encapsulate call to get the set of running jobs and make a copy of it inside a throwable
     * since concurrency is an issues.
     * @return a copy of the running jobs set
     */
    public Set<Long> getRunningJobs() {
        try {
            @SuppressWarnings("unchecked")
            Set<Long> orgJobs = harvestMonitor.getRunningJobs();
            Set<Long> jobs = new TreeSet<Long>(orgJobs);
            return jobs;
        } catch (Throwable t) {
            LOG.debug("Heritrix3 Job Monitor Thread cloning of running jobs failed with an exception!", t);
            return null;
        }
    }

    public Heritrix3JobMonitor getRunningH3Job(long jobId) {
        Heritrix3JobMonitor h3Job;
        synchronized (runningJobMonitorMap) {
            h3Job = runningJobMonitorMap.get(jobId);
            LOG.debug("runningJobMonitorMap.hashCode() from Heritrix3JobMonitorThread.getRunningH3Job(long jobId)",
                    runningJobMonitorMap.hashCode());
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

    public void checkH3HostnamePort(Heritrix3JobMonitor jobmonitor) {
        Heritrix3Wrapper h3wrapper = jobmonitor.h3wrapper; 
        if (jobmonitor.h3HostnamePort == null && h3wrapper != null) {
            synchronized (h3HostPortSet) {
                jobmonitor.h3HostnamePort = h3wrapper.hostname + ":" + h3wrapper.port;
                if (!h3HostPortSet.contains(jobmonitor.h3HostnamePort)) {
                    h3HostPortSet.add(jobmonitor.h3HostnamePort);
                    updateH3HostnamePortFilter();
                }
            }
        }
    }

    public boolean isH3HostnamePortEnabled(Heritrix3JobMonitor jobmonitor) {
        synchronized (h3HostnamePortEnabledList) {
            // TODO Not ideal to do contains on a list. But its fairly short.
            jobmonitor.bPull = h3HostnamePortEnabledList.contains(jobmonitor.h3HostnamePort);
        }
        return jobmonitor.bPull;
    }

    public void updateH3HostnamePortFilter() {
        String h3HostnamePort;
        List<String> enabledList = new LinkedList<String>();
        List<String> disabledList = new LinkedList<String>();
        synchronized (h3HostPortSet) {
            Iterator<String> iter = h3HostPortSet.iterator();
            while (iter.hasNext()) {
                h3HostnamePort = iter.next();
                if (environment.isH3HostnamePortEnabled(h3HostnamePort)) {
                    enabledList.add(h3HostnamePort);
                } else {
                    disabledList.add(h3HostnamePort);
                }
            }
        }
        synchronized (h3HostnamePortEnabledList) {
            h3HostnamePortEnabledList.clear();
            h3HostnamePortEnabledList.addAll(enabledList);
            Collections.sort(h3HostnamePortEnabledList);
        }
        synchronized (h3HostnamePortDisabledList) {
            h3HostnamePortDisabledList.clear();
            h3HostnamePortDisabledList.addAll(disabledList);
            Collections.sort(h3HostnamePortDisabledList);
        }
    }

}
