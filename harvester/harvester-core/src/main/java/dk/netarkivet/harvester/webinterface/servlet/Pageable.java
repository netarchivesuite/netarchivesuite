package dk.netarkivet.harvester.webinterface.servlet;

import java.io.IOException;

public interface Pageable {

    public long getIndexSize();

    public byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException;

}
