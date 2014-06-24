
package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import javax.inject.Provider;

/**
 * Handles the dispatching of scheduled harvest to the harvest servers based on
 * the harvests defined in the database. <p>
 */
public class HarvestJobManager extends LifeCycleComponent {
    private final JMSConnection jmsConnection;

    /**
     * Creates the components handling the harvest job management and hooks them
     * up to the <code>HarvestJobManager</code>s lifecycle.
     */
    public HarvestJobManager() {
        jmsConnection = JMSConnectionFactory.getInstance();       
        JobDispatcher jobDispather = new JobDispatcher(
                jmsConnection,
                HarvestDefinitionDAO.getInstance(),
                JobDAO.getInstance()
        );
        HarvestChannelRegistry harvestChannelRegistry = new HarvestChannelRegistry();

        addChild(new HarvesterStatusReceiver(
                jobDispather,
                jmsConnection,
                HarvestChannelDAO.getInstance(),
                harvestChannelRegistry));
        
        addChild(new HarvestJobGenerator(harvestChannelRegistry));
        
        addChild(new HarvestSchedulerMonitorServer());
        
        addChild(new JobSupervisor(createJobDaoProvider(),
                Settings.getLong(HarvesterSettings.JOB_TIMEOUT_TIME)));
    }

    @Override
    public void shutdown() {
        super.shutdown();

        // Release DB resources
        HarvestDBConnection.cleanup();
    }

    private Provider<JobDAO> createJobDaoProvider() {
        return new Provider<JobDAO>() {
            @Override
            public JobDAO get() {
                return JobDAO.getInstance();
            }
        };
    }
}
