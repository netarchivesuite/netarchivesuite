package dk.netarkivet.common.distribute.bitrepository.action.getfileids;

import org.bitrepository.access.getfileids.conversation.FileIDsCompletePillarEvent;
import org.bitrepository.bitrepositoryelements.FileIDsData;
import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.ContributorFailedEvent;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.IdentificationCompleteEvent;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.client.eventhandler.OperationFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetFileIDsEventHandler implements EventHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean partialResults = false;
    private final Object finishLock = new Object();
    private boolean finished = false;
    private boolean failed = false;
    private FileIDsData fileIDsData = null;

    private final String pillarID;

    /**
     * Constructor
     * @param pillarID The pillar which is expected to deliver the results.
     */
    public GetFileIDsEventHandler(String pillarID) {
        this.pillarID = pillarID;
    }

    @Override
    public void handleEvent(OperationEvent event) {
        logIfFinished(event);
        log.info("Got event from client: {} ({}), {} for conversation {}.", event.getEventType(), event.getClass(), event.getInfo(), event.getConversationID());
        if (event instanceof ContributorFailedEvent) {
            log.info("Additional info: {} for conversation {}.", ((ContributorFailedEvent) event).additionalInfo(), event.getConversationID());
        }
        switch(event.getEventType()) {
        case COMPONENT_COMPLETE:
            log.debug("Got COMPONENT_COMPLETE event {} for conversation {}.", event, event.getConversationID());
            if (event instanceof FileIDsCompletePillarEvent) {
                FileIDsCompletePillarEvent getFileIDsEvent = (FileIDsCompletePillarEvent) event;
                if (getFileIDsEvent.getContributorID().equals(pillarID)) {
                    fileIDsData = getFileIDsEvent.getFileIDs().getFileIDsData();
                    partialResults = getFileIDsEvent.isPartialResult();
                    log.info("Received {} results (partialResults = {}) for {}.",
                            getFileIDsData().getFileIDsDataItems().getFileIDsDataItem().size(), partialResults, event.getConversationID());
                } else {
                    log.warn("Got an event from an unexpected contributor '{}' expected '{}' for {}.",
                            getFileIDsEvent.getContributorID(), pillarID, event.getConversationID());
                }
            }
            break;
        case IDENTIFICATION_COMPLETE:
            log.info("Identification complete for {} (class is {})", event.getConversationID(), event.getClass());
            break;
        case IDENTIFY_TIMEOUT:
            log.info("Timed out on identify for conversation {}.", event.getConversationID());
            failed = true;
            finish();  //?? Is this necessary? Don't we get a FAILED after this?
            break;
        case IDENTIFY_REQUEST_SENT:
            log.info("Received Identify Request Sent event {} for {}", event.getClass(), event.getConversationID());
            break;
        case COMPLETE:
            log.info("Finished getting fileIDs from pillar '{}', {}", pillarID, event.getConversationID());
            finish();
            break;
        case FAILED:
            log.warn("Failed getting fileIDs from pillar '{}', {}", pillarID, event.getConversationID());
            if (event instanceof OperationFailedEvent) {
                for (ContributorEvent contributorEvent: ((OperationFailedEvent) event).getComponentResults()) {
                    log.info("During GetFileIDs event {} from {} had status {} in conversation {}.",
                            contributorEvent.getInfo(),
                            contributorEvent.getContributorID(),
                            contributorEvent.additionalInfo(),
                            event.getConversationID()
                    );
                }
            }
            failed = true;
            finish();
            break;
        default:
            break;
        }
    }

    /**
     * Method to obtain the received fileids data.
     * The method should not be called prior to a call to {@link #waitForFinish()} have returned.
     * @return The fileids data if it have been returned by the pillar, otherwise null.
     */
    public FileIDsData getFileIDsData() {
        return fileIDsData;
    }

    private void logIfFinished(OperationEvent event) {
        if (finished) {
            log.info("CAREFUL! The following is an out-of-sync message for an event which we are finished handling: {}, {}", event
                    .getConversationID());
        }
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
            if (!finished) {
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
