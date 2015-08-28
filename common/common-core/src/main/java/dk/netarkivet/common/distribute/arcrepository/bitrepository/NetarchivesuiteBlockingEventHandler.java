package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.util.List;
import java.util.logging.Logger;

import org.bitrepository.client.eventhandler.BlockingEventHandler;
import org.bitrepository.common.utils.SettingsUtils;

/**
 * Eventhandler for Netarchivesuite
 * Extends the BlockingEventHandler by allowing failures of some pillars.
 */
public class NetarchivesuiteBlockingEventHandler extends BlockingEventHandler {
    /** Logging mechanism. */
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /** The list of pillars.*/
    private final List<String> pillars;
    /** The maximum number of pillars to give a failure.*/
    private final int maxFailures;

    /**
     * Constructor.
     * @param collectionId The id for the collection where the events occur.
     * @param maxNumberOfFailures The maximum number of failures.
     */
    public NetarchivesuiteBlockingEventHandler(String collectionId, int maxNumberOfFailures) {
        super();
        this.pillars = SettingsUtils.getPillarIDsForCollection(collectionId);
        this.maxFailures = maxNumberOfFailures;
    }

    @Override
    public boolean hasFailed() {
        if(super.hasFailed()) {
            // Fail, if no final events from all pillars.
            if(pillars.size() > (getFailures().size() + getResults().size())) {
                logger.warning("Some pillar(s) have neither given a failure or a complete. Expected: " 
                        + pillars.size() + ", but got: " + (getFailures().size() + getResults().size()));
                return true;
            }
            // Fail, if more failures than allowed.
            if(maxFailures < getFailures().size()) {
                logger.warning("More failing pillars than allowed. Max failures allowed: " + maxFailures 
                        + ", but " + getFailures().size() + " pillars failed.");
                return true;
            }
            // Accept, when less failures than allowed, and the rest of the pillars have success.
            if((pillars.size() - maxFailures) <= getResults().size()) {
                logger.info("Only " + getFailures().size() + " pillar(s) failed, and we accept " 
                        + maxFailures + ", so is a success.");
                return false;
            } else {
                logger.severe("Less failures than allowed, and less successes than required, but not failures and "
                        + "successes combined are at least the number of pillars. This should never happen!");
                return true;
            }
        }
        return false;
    }
}
