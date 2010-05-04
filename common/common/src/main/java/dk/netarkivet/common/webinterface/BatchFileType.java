package dk.netarkivet.common.webinterface;

public enum BatchFileType {
    // The metadata files.
    Metadata,
    // The content files (those not metadata)
    Content,
    // Both metadata and content files.
    Both;

}
