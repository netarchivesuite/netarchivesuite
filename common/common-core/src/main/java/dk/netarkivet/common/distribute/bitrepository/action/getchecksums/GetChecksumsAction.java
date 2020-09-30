package dk.netarkivet.common.distribute.bitrepository.action.getchecksums;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.bitrepository.access.ContributorQuery;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.action.ClientAction;

/**
 * Action class to get checksums from Bitmag.
 */
public class GetChecksumsAction implements ClientAction {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT);
    
    private final GetChecksumsClient checksumsClient;
    private final String collectionID;
    private final String pillarID;
    private final String fileID;
    // TODO Add checksums together with date and fileID to some map or other structure? Need to know how this is going to be used.

    public GetChecksumsAction(GetChecksumsClient checksumsClient, String collectionID, String pillarID, String fileID) {
        this.checksumsClient = checksumsClient;
        this.collectionID = collectionID;
        this.pillarID = pillarID;
        this.fileID = fileID;
    }

    @Override
    public void performAction() {
        GetChecksumsEventHandler eventHandler = new GetChecksumsEventHandler(pillarID);
        checksumsClient.getChecksums(collectionID, getQuery(pillarID, new Date(0)), fileID, 
                BitmagUtils.getChecksumSpec(ChecksumType.MD5), null, eventHandler, null);
        
        try {
            eventHandler.waitForFinish();
            
            List<ChecksumDataForChecksumSpecTYPE> checksumData = eventHandler.getChecksumData();
            System.out.println("Checksum:\t CalculationTime:\t FileID:");    
            for (ChecksumDataForChecksumSpecTYPE cd : checksumData) {
                Date calculationDate = CalendarUtils.convertFromXMLGregorianCalendar(cd.getCalculationTimestamp());
                String checksum = Base16Utils.decodeBase16(cd.getChecksumValue());
                System.out.printf(Locale.ROOT, "%s\t %s\t %s%n", checksum, sdf.format(calculationDate), cd.getFileID());
            }

        } catch (InterruptedException e) {
            log.error("Got interrupted while waiting for operation to complete");
        }
        
    }
    
    private ContributorQuery[] getQuery(String pillarID, Date minDate) {
        List<ContributorQuery> res = new ArrayList<ContributorQuery>();
        res.add(new ContributorQuery(pillarID, minDate, null, 10000));
        return res.toArray(new ContributorQuery[1]);
    } 

}
