package dk.netarkivet.common.distribute.bitrepository.action.putfile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.ContributorFailedEvent;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.IdentificationCompleteEvent;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.protocol.FileExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.utils.Settings;

public class PutFileEventHandler implements EventHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final FileExchange fileExchange = BitmagUtils.getFileExchange();
    private final List<String> pillars;
    private final File targetFile;
    private final URL uploadURL;
    private final List<ContributorEvent> componentCompleteEvents = new ArrayList<>();
    private final List<ContributorFailedEvent> componentFailedEvents = new ArrayList<>();
    private final Object finishLock = new Object();
    private boolean finished = false;
    private boolean failed = false;

    /**
     * Constructor
     * @param pillars A list of pillars the file is put to
     * @param targetFile The file to put in the bitrepository
     * @param uploadURL The url where the file is uploaded to if the action is successful
     */
    public PutFileEventHandler(List<String> pillars, File targetFile, URL uploadURL) {
        this.pillars = pillars;
        this.targetFile = targetFile;
        this.uploadURL = uploadURL;
    }

    @Override
    public void handleEvent(OperationEvent event) {
        log.info("Got event from client: {}", event.getEventType());
        switch (event.getEventType()) {
        case IDENTIFICATION_COMPLETE:
            IdentificationCompleteEvent completeEvent = (IdentificationCompleteEvent) event;
            if (completeEvent.getContributorIDs().size() > 0) {
                uploadToFileExchange();
            }
            break;
        case COMPLETE:
            log.info("Finished put fileID for file '{}'", event.getFileID());
            cleanUpFileExchange();
            finish();
            break;
        case FAILED:
            log.info("Failed put fileID for file '{}'", event.getFileID());
            failed = true;
            cleanUpFileExchange();
            finish();
            break;
        case COMPONENT_COMPLETE:
            componentCompleteEvents.add((ContributorEvent) event);
            break;
        case COMPONENT_FAILED:
            componentFailedEvents.add((ContributorFailedEvent) event);
            break;
        default:
            break;
        }
    }

    private void uploadToFileExchange() {
        int bufferSize = 16384;
        try (InputStream is = new BufferedInputStream(new FileInputStream(targetFile), bufferSize)) {
            fileExchange.putFile(is, uploadURL);
            log.debug("Finished uploading file '{}' to file exchange", targetFile.getName());
        } catch (IOException e) {
            log.error("Failed to upload file '{}' to file exchange", targetFile.getName());
        }
    }

    private void cleanUpFileExchange() {
        try {
            fileExchange.deleteFile(uploadURL);
            log.debug("Finished cleaning up '{}' at URL: '{}'..", targetFile.getName(), uploadURL.toExternalForm());
        } catch (IOException | URISyntaxException e) {
            log.error("Failed cleaning up '{}' at URL: '{}'..", targetFile.getName(), uploadURL.toExternalForm());
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
            if(!finished) {
                log.trace("Thread waiting for client to finish");
                finishLock.wait();
            }
            log.trace("Client have indicated it's finished.");
        }
    }

    /**
     * Method to determine if the operation was successful.
     * Unlike the other operations, the putFile operation allows some failures determined by a setting.
     * The method should not be called prior to a call to {@link #waitForFinish()} have returned.
     * @return true if the operation succeeded, otherwise false.
     */
    public boolean hasFailed() {
        if (failed) {
            int maxFailures = Settings.getInt(BitmagUtils.BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
            // Fail, if no final events from all pillars.
            if (pillars.size() > (componentFailedEvents.size() + componentCompleteEvents.size())) {
                log.warn("Some pillar(s) have neither given a failure or a complete. Expected: {}, but got: {}",
                        pillars.size(), componentFailedEvents.size() + componentCompleteEvents.size());
                return true;
            }
            // Fail, if more failures than allowed.
            if (componentFailedEvents.size() > maxFailures) {
                log.error("More failing pillars than allowed. Max failures allowed: {}, but {} pillars failed.",
                        maxFailures, componentFailedEvents.size());
                return true;
            }
            // Accept, when less failures than allowed, and the rest of the pillars have success.
            if (componentCompleteEvents.size() >= (pillars.size() - maxFailures)) {
                log.info("Only {} pillar(s) failed, and we accept {}, so the operation is a success.",
                        componentFailedEvents.size(), maxFailures);
                return false;
            } else {
                log.error("Fewer failures than allowed, and fewer successes than required, but failures and "
                        + "successes combined are at least the number of pillars. This should never happen!");
                return true;
            }
        }
        return false;
    }
}
