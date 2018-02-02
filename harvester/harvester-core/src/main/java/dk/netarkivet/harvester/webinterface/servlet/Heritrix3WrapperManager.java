package dk.netarkivet.harvester.webinterface.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jwat.common.Uri;
import org.netarchivesuite.heritrix3wrapper.EngineResult;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.jaxb.JobShort;

public class Heritrix3WrapperManager {

    protected Heritrix3WrapperManager() {
    }

    public static final Map<String, Heritrix3Wrapper> h3wrapperMap = new HashMap<String, Heritrix3Wrapper>();

    public static Heritrix3Wrapper getHeritrix3Wrapper(String h3EngineUrl, String username, String password) {
        Heritrix3Wrapper h3wrapper = null;
        if (h3EngineUrl != null) {
            synchronized (h3wrapperMap) {
                h3wrapper = h3wrapperMap.get(h3EngineUrl);
                if (h3wrapper == null) {
                    Uri uri = Uri.create(h3EngineUrl);
                    String scheme = uri.getScheme();
                    String hostname = uri.getHost();
                    int port = uri.getPort();
                    if (port == -1) {
                        if ("https".equalsIgnoreCase(scheme)) {
                            port = 443;
                        } else {
                            port = 80;
                        }
                    }
                    h3wrapper = Heritrix3Wrapper.getInstance(hostname, port, null, null, username, password);
                    h3wrapperMap.put(h3EngineUrl, h3wrapper);
                }
            }
        }
        return h3wrapper;
    }

    public static final Map<Long, String> h3jobnameMap = new TreeMap<Long, String>();

    public static String getJobname(Heritrix3Wrapper h3wrapper, long jobId) {
        String jobname;
        synchronized (h3jobnameMap) {
            jobname = h3jobnameMap.get(jobId);
            if (jobname == null) {
                EngineResult engineResult = h3wrapper.rescanJobDirectory();
                JobShort jobShort = null;
                if (engineResult != null && engineResult.engine != null) {
                    List<JobShort> jobList = engineResult.engine.jobs;
                    JobShort tmpJobShort;
                    String jobPostFix = Long.toString(jobId) + "_";
                    int idx = 0;
                    while (idx < jobList.size() && jobShort == null) {
                        tmpJobShort = jobList.get(idx++);
                        if (tmpJobShort.shortName.startsWith(jobPostFix)) {
                            jobShort = tmpJobShort;
                        }
                    }
                }
                if (jobShort != null) {
                    jobname = jobShort.shortName;
                    h3jobnameMap.put(jobId, jobname);
                }
            }
        }
        return jobname;
    }

    public static final Map<Long, Heritrix3JobMonitor> h3JobmonitorMap = new TreeMap<Long, Heritrix3JobMonitor>();

    public static Heritrix3JobMonitor getJobMonitor(long jobId, NASEnvironment environment) throws IOException {
        Heritrix3JobMonitor jobmonitor;
        synchronized (h3JobmonitorMap) {
            jobmonitor = h3JobmonitorMap.get(jobId);
            if (jobmonitor == null) {
                jobmonitor = Heritrix3JobMonitor.getInstance(jobId, environment);
                h3JobmonitorMap.put(jobId, jobmonitor);
            }
        }
        return jobmonitor;
    }

}
