package dk.netarkivet.archive.arcrepositoryadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Assert;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import junit.framework.TestCase;

public class FileListStatusTest {

    public void testFromOrdinal() {
        assertEquals(FileListStatus.NO_FILELIST_STATUS, FileListStatus.fromOrdinal(0));
        assertEquals(FileListStatus.MISSING, FileListStatus.fromOrdinal(1));
        assertEquals(FileListStatus.OK, FileListStatus.fromOrdinal(2));
        try {
            FileListStatus.fromOrdinal(3);
            fail("Should throw ArgumentNotValid with argument > 2");
        } catch (ArgumentNotValid e) {
            // Expected
        }

    }
}
