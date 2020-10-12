package dk.netarkivet.common.distribute.bitrepository.action.getchecksums;

import java.util.List;

import org.bitrepository.access.getchecksums.conversation.ChecksumsCompletePillarEvent;
import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler to handle results from a GetChecksums request made to a single pillar. 
 */
public class GetChecksumsEventHandler implements EventHandler {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean partialResults = false;
    private final Object finishLock = new Object();
    private boolean finished = false;
    private boolean failed = false;
    private List<ChecksumDataForChecksumSpecTYPE> checksumData = null;

    private String pillarID;
    
    /**
     * Constructor
     * @param pillarID The pillar which is expected to deliver the results.  
     */
    public GetChecksumsEventHandler(String pillarID) {
        this.pillarID = pillarID;
    }
    
    @Override
    public void handleEvent(OperationEvent event) {
        log.info("Got event from client: {}", event.getEventType());
        switch(event.getEventType()) {
        case COMPONENT_COMPLETE:
            log.debug("Got COMPONENT_COMPLETE event {}", event);
            if(event instanceof ChecksumsCompletePillarEvent) {
                ChecksumsCompletePillarEvent checksumsEvent = (ChecksumsCompletePillarEvent) event;
                if(checksumsEvent.getContributorID().equals(pillarID)) {
                    checksumData = checksumsEvent.getChecksums().getChecksumDataItems();
                    partialResults = checksumsEvent.isPartialResult();
                } else {
                    log.warn("Got an event from an unexpected contributor '{}' expected '{}'", 
                            checksumsEvent.getContributorID(), pillarID);
                }
            }
            case COMPLETE:
                log.info("Finished get checksum for file '{}'", event.getFileID());
                finish();
                break;
            case FAILED:
                log.warn("Failed get checksum for file '{}'", event.getFileID());
                failed = true;
                finish();
                break;
            default:
                break;
            }    
    }
    
    /**
     * Method to obtain the received checksum data. 
     * The method should not be called prior to a call to {@link #waitForFinish()} have returned. 
     * @return The checksum data if it have been returned by the pillar, otherwise null.   
     */
    public List<ChecksumDataForChecksumSpecTYPE> getChecksumData() {
        return checksumData;
    }
    
    /**
     * Method to indicate the operation have finished (regardless if is successful or not). 
     */
    private void finish() {
        log.trace("Finish method invoked");
        synchronized (finishLock) {
            log.trace("Finish method entered synchronized block");
            finished = true;
            finishLock.notifyAll();
            log.trace("Finish method notified All");            
        }
    }

    /**
     * Method to wait for the operation to complete. The method is blocking.  
     * @throws InterruptedException if the thread is interrupted
     */
    public void waitForFinish() throws InterruptedException {
        synchronized (finishLock) {
            if(finished == false) {
                log.trace("Thread waiting for client to finish");
                finishLock.wait();
            }
            log.trace("Client have indicated it's finished.");
        }
    }
    
    /**
     * Method to determine if the received results is a partial result set. I.e. should
     * the client send a new request to get more results. 
     * The method should not be called prior to a call to {@link #waitForFinish()} have returned.
     * @return true if the results are partial   
     */
    public boolean partialResults() {
        return partialResults;
    }
    
    /**
     * Method to determine if the operation was successful.
     * The method should not be called prior to a call to {@link #waitForFinish()} have returned. 
     * @return true if the operation succeeded, otherwise false. 
     */
    public boolean hasFailed() {
        return failed;
    }
 
}
