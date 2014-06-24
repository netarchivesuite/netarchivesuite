package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;

/**
 * Class needed to test the constructor for FilePreservationStatus, which takes
 * an ArcRepositoryEntry as one of its arguments.
 * The constructor of ArcRepositoryEntry is package private.
 *
 *
 */
public class MyArcRepositoryEntry extends ArcRepositoryEntry {

    public MyArcRepositoryEntry(String filename, String md5sum,
            StoreMessage replyInfo) {
        super(filename, md5sum, replyInfo);

    }

}
