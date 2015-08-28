package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import java.util.UUID;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.ChecksumUtils;

/** Utilities used by the Bitrepository class. */
public class BitrepositoryUtils {
    /**
     * Creates the data structure for encapsulating the validation checksums for validation of the PutFile operation.
     * @param file The file to have the checksum calculated.
     * @param csSpec A given ChecksumSpecTYPE
     * @return The ChecksumDataForFileTYPE for the pillars to validate the PutFile operation.
     */
    public static ChecksumDataForFileTYPE getValidationChecksum(File file, ChecksumSpecTYPE csSpec) {
        //ArgumentCheck.checkExistsNormalFile(file, "File file");
        //ArgumentCheck.checkNotNull(csSpec, "ChecksumSpecTYPE csSpec");
        String checksum = ChecksumUtils.generateChecksum(file, csSpec);
        ChecksumDataForFileTYPE res = new ChecksumDataForFileTYPE();
        res.setCalculationTimestamp(CalendarUtils.getNow());
        res.setChecksumSpec(csSpec);
        res.setChecksumValue(Base16Utils.encodeBase16(checksum));
        return res;
    }

    /**
     *  Specify a checksum.
     *  @param checksumtype a given type of checksum
     *  @param salt A string to salt the checksum with (if null, no salting)
     * @return The requested checksum spec
     */
    public static ChecksumSpecTYPE getRequestChecksumSpec(ChecksumType checksumtype, String salt) {
        ChecksumSpecTYPE res = new ChecksumSpecTYPE();
        res.setChecksumType(checksumtype);
        if (salt != null) {
            res.setChecksumSalt(Base16Utils.encodeBase16(salt));
        }
        return res;
    }

    /**
     * Generates a component id, which includes the hostname and a random UUID.
     * @return The Bitrepository component id for this instance of Yggdrasil.
     */
    public static String generateComponentID() {
        String hn = HostName.getHostName();
        return "NetarchivesuiteClient-" + hn + "-" + UUID.randomUUID();
    }
}
