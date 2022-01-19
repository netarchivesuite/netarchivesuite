package dk.netarkivet.common.distribute.bitrepository.action.getfileids;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bitrepository.access.ContributorQuery;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.FileIDsData;
import org.bitrepository.bitrepositoryelements.FileIDsDataItem;
import org.bitrepository.common.utils.CalendarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.action.ClientAction;
import dk.netarkivet.common.utils.Settings;

/**
 * Action class to get file IDs from Bitmag.
 */
public class GetFileIDsAction implements ClientAction {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final GetFileIDsClient client;
    private final String collectionID;
    private final String pillarID;
    private final Date minDate;
    private final Set<String> fileIDs = new HashSet<>();

    /**
     * Constructor to instantiate the get-file-ids action
     * @param client The client to perform the action on
     * @param collectionID The ID of a known collection to operate on
     * @param pillarID The ID of a known pillar to fetch the file IDs from
     * @param minDate The date specifying how old file ids to fetch
     */
    public GetFileIDsAction(GetFileIDsClient client, String collectionID, String pillarID, Date minDate) {
        this.client = client;
        this.collectionID = collectionID;
        this.pillarID = pillarID;
        this.minDate = minDate;
    }

    @Override
    public void performAction() {
        log.info("Performing getFileIds action from {} since date {}.", pillarID, minDate);
        GetFileIDsEventHandler eventHandler = new GetFileIDsEventHandler(pillarID);
        Date latestDate = minDate;
        boolean newDate = true;

        do {
            if (newDate) {
                log.info("Performing incremental getFileIds action from {} since date {}.", pillarID, latestDate);
                client.getFileIDs(collectionID, getQuery(pillarID, latestDate, new Date(System.currentTimeMillis())),
                        null, null, eventHandler);
                newDate = false;
            } else {
                log.info("Not incrementing getFileIds because latest date not increased.");
                break;
            }
            try {
                eventHandler.waitForFinish();
                if (eventHandler.hasFailed()) {
                    log.warn("Failed getting fileIDs from pillar '{}'.", pillarID);
                    return;
                }

                FileIDsData fileIDsData = eventHandler.getFileIDsData();
                log.info("Found {} fileIds",fileIDsData.getFileIDsDataItems().getFileIDsDataItem().size());
                for (FileIDsDataItem fileIDsDataItem : fileIDsData.getFileIDsDataItems().getFileIDsDataItem()) {
                    String fileID = fileIDsDataItem.getFileID();
                    Date lastModificationDate = CalendarUtils.convertFromXMLGregorianCalendar(fileIDsDataItem.getLastModificationTime());
                    log.debug("FileId {}, mtime {}, lastModificationDate {}, {}", fileID, fileIDsDataItem.getLastModificationTime(), lastModificationDate, lastModificationDate.getTime());
                    if (lastModificationDate.after(latestDate)) {
                        log.debug("Updating latestDate to {}, {}", lastModificationDate, lastModificationDate.getTime());
                        latestDate = lastModificationDate;
                        newDate = true;
                    }
                    fileIDs.add(fileID);
                }
                log.info("Total distinct fileIds now found is {}", fileIDs.size());
            } catch (InterruptedException e) {
                log.error("Got interrupted while waiting for operation to complete");
            }
        } while (eventHandler.partialResults());
    }

    private ContributorQuery[] getQuery(String pillarID, Date minDate, Date maxDate) {
        log.info("Constructing contributor query {},{} ({},{})", minDate, maxDate, minDate.getTime(), maxDate.getTime());
        List<ContributorQuery> res = new ArrayList<ContributorQuery>();
        int maxResults = Settings.getInt(BitmagUtils.BITREPOSITORY_GETFILEIDS_MAX_RESULTS);
        res.add(new ContributorQuery(pillarID, minDate, maxDate, maxResults));
        return res.toArray(new ContributorQuery[1]);
    }

    public Set<String> getActionResult() {
        return fileIDs;
    }
}
