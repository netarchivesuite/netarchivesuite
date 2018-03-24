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

    public static Map<String, Heritrix3Wrapper> h3wrapperMap = new HashMap<String, Heritrix3Wrapper>();

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
                        	// Assume schema is http.
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

    public static Map<Long, String> h3jobnameMap = new TreeMap<Long, String>();

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

    public static Map<Long, Heritrix3JobMonitor> h3JobmonitorMap = new TreeMap<Long, Heritrix3JobMonitor>();

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
