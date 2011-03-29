/**
 *
 */
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;

/**
 *
 *
 */
public class ReadyForJobMessage
extends HarvesterMessage
implements Serializable {

    private final JobPriority jobProprity;

    /**
     *
     * @param jobPriority
     */
    public ReadyForJobMessage(JobPriority jobPriority) {
        super(Channels.getHarvestDispatcherChannel(), Channels.getError());
        this.jobProprity = jobPriority;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the jobProprity
     */
    public JobPriority getJobProprity() {
        return jobProprity;
    }

}
