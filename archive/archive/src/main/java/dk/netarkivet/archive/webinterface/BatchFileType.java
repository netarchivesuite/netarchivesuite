package dk.netarkivet.archive.webinterface;

/**
 * Enumerator for the different types of files the batchjob can be executed
 * upon.
 */
public enum BatchFileType {
    /** The metadata files.*/
    Metadata,
    /** The content files (those not metadata).*/
    Content,
    /** Both metadata and content files.*/
    Both;

}
