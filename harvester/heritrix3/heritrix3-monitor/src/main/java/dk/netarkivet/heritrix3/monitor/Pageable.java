package dk.netarkivet.heritrix3.monitor;

import java.io.IOException;

public interface Pageable {

    public long getIndexSize();

    public long getLastIndexed();

    public byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException;

}
