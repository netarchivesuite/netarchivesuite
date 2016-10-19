package dk.netarkivet.harvester.webinterface.servlet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class SearchResult implements Pageable {

    protected Heritrix3JobMonitor h3Job;

    protected Pattern p;
    protected Matcher m;

    protected File srIdxFile;

    protected RandomAccessFile idxRaf;

    protected long lastIndex;

    public SearchResult(Heritrix3JobMonitor h3Job, String q, int searchResultNr) throws IOException {
        this.h3Job = h3Job;
        p = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
        m = p.matcher("42");
        srIdxFile = new File("crawllog-" + h3Job.jobId + "-" + searchResultNr + ".idx");
        idxRaf = new RandomAccessFile(srIdxFile, "rw");
        idxRaf.setLength(0);
        lastIndex = 0;
    }

    public synchronized void update() throws IOException {
        RandomAccessFile logRaf = new RandomAccessFile(h3Job.logFile, "r");
        idxRaf.seek(idxRaf.length());
        logRaf.seek(lastIndex);
        FileChannel logChannel = logRaf.getChannel();
        byte[] bytes = new byte[1024*1024];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        String tmpStr;
        long index = lastIndex;
        int pos;
        int to;
        int mark;
        int limit;
        boolean b;
        while (logChannel.read(byteBuffer) != -1) {
            byteBuffer.flip();
            pos = byteBuffer.position();
            mark = pos;
            limit = byteBuffer.limit();
            b = true;
            while (b) {
                if (pos < limit) {
                    if (bytes[pos++] == '\n') {
                        to = pos - 1;
                        if (bytes[to - 1] == '\r') {
                            --to;
                        }
                        tmpStr = new String(bytes, mark, to - mark, "UTF-8");
                        m.reset(tmpStr);
                        if (m.matches()) {
                            idxRaf.writeLong(index);
                        }
                        // next
                        mark = pos;
                        index += mark - pos;
                        lastIndex = index;
                    }
                } else {
                    b = false;
                }
            }
        }
        logRaf.close();
    }

    @Override
    public long getIndexSize() {
        return srIdxFile.length();
    }

    @Override
    public synchronized byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException {
        RandomAccessFile logRaf = new RandomAccessFile(h3Job.logFile, "r");
        return StringIndexFile.readPage(idxRaf, logRaf, page, itemsPerPage, descending);
    }

    public synchronized void cleanup() {
        IOUtils.closeQuietly(idxRaf);
    }

}
