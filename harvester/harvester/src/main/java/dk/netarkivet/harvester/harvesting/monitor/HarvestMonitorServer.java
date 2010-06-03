/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.harvesting.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jms.MessageListener;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;

/**
 * Listens for {@link CrawlProgressMessage}s on the proper JMS channel, and
 * stores information to be presented in the monitoring console.
 */
public class HarvestMonitorServer extends HarvesterMessageHandler implements
        MessageListener {

    /**
     * A "grim reaper" that periodically wipes the started job infos.
     */
    private static class WiperThread extends Thread {

        boolean alive = true;

        long cleanInterval = Settings
                .getLong(HarvesterSettings.HARVEST_MONITOR_RESET_INTERVAL);

        @Override
        public void run() {
            while (alive) {
                instance.cleanJobInfos();
                try {
                    Thread.sleep(1000 * cleanInterval);
                } catch (InterruptedException e) {

                }
            }
        }

        public void kill() {
            alive = false;
        }

    }

    /**
     * Singleton instance of the monitor.
     */
    private static HarvestMonitorServer instance;

    /**
     * The JMS channel on which to listen for {@link CrawlProgressMessage}s.
     */
    public static final ChannelID JMS_CHANNEL_ID = Channels
            .getHarvestMonitorServerChannel();

    /**
     * Maps instances of {@link StartedJobInfo} to the associated job ID.
     */
    private Map<Long, StartedJobInfo> jobInfosByJobId
        = new HashMap<Long, StartedJobInfo>();

    /**
     * The "grim reaper" thread.
     */
    private WiperThread wipe;

    private HarvestMonitorServer() {
        JMSConnectionFactory.getInstance().setListener(JMS_CHANNEL_ID, this);
        wipe = new WiperThread();
        wipe.start();
    }

    @Override
    protected void finalize() throws Throwable {
        wipe.kill();
        wipe.join();
        super.finalize();
    }

    /**
     * @return the singleton instance for this class.
     */
    public static HarvestMonitorServer getInstance() {
        if (instance == null) {
            instance = new HarvestMonitorServer();
        }
        return instance;
    }

    /**
     * Close down the HarvestSchedulerMonitorServer singleton. This removes the
     * HarvestSchedulerMonitorServer as listener to the JMS scheduler Channel,
     * and resets the singleton.
     * 
     */
    public void close() {
        JMSConnectionFactory.getInstance().removeListener(JMS_CHANNEL_ID, this);
        cleanup();
    }

    /**
     * Cleanup method. Resets HarvestSchedulerMonitorServer singleton. Note:
     * this cleanup() method is called from HarvestScheduler.cleanup(),
     * therefore it needs to be public
     */
    public void cleanup() {
        instance = null;
    }

    @Override
    public void visit(CrawlProgressMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        Long jobId = Long.valueOf(msg.getJobID());

        JobStatus jobStatus = JobDAO.getInstance().read(jobId).getStatus();
        if (!JobStatus.STARTED.equals(jobStatus)) {
            return;
        }

        StartedJobInfo info = jobInfosByJobId.get(jobId);
        if (info == null) {
            info = new StartedJobInfo(msg.getHarvestID(), jobId);
            jobInfosByJobId.put(jobId, info);
        }

        info.update(msg);
    }

    /**
     * @return the full listing of started job information.
     */
    public StartedJobInfo[] getAllJobInfos() {
        TreeSet<StartedJobInfo> infos = new TreeSet<StartedJobInfo>();
        infos.addAll(jobInfosByJobId.values());
        return (StartedJobInfo[]) infos
                .toArray(new StartedJobInfo[infos.size()]);
    }

    /**
     * @return the full listing of started job information, partitioned by
     *         harvest definition name.
     */
    public Map<String, Set<StartedJobInfo>> getJobInfosByHarvestName() {

        TreeMap<String, Set<StartedJobInfo>> infoMap 
            = new TreeMap<String, Set<StartedJobInfo>>();
        for (StartedJobInfo i : jobInfosByJobId.values()) {
            String harvestName = i.getHarvestName();
            Set<StartedJobInfo> jobInfos = infoMap.get(harvestName);
            if (jobInfos == null) {
                jobInfos = new TreeSet<StartedJobInfo>();
                infoMap.put(harvestName, jobInfos);
            }
            jobInfos.add(i);
        }
        return infoMap;
    }

    /**
     * Removes from the list of started jobs any job whose status is not started
     * (finished or erroneous jobs).
     */
    private void cleanJobInfos() {

        // Fetch the IDs of started jobs
        List<JobStatusInfo> startedJobs = JobDAO.getInstance().getStatusInfo(
                JobStatus.STARTED);
        List<Long> startedJobIds = new ArrayList<Long>();
        for (JobStatusInfo startedJob : startedJobs) {
            startedJobIds.add(startedJob.getJobID());
        }

        // Iterate on monitored jobs, mark zombie records
        List<Long> idsToRemove = new ArrayList<Long>();
        for (Long jobId : jobInfosByJobId.keySet()) {
            if (!startedJobIds.contains(jobId)) {
                idsToRemove.add(jobId);
            }
        }
        // Remove zombies
        for (Long jobId : idsToRemove) {
            jobInfosByJobId.remove(jobId);
        }
    }

}
