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

    protected File srLogFile;

    protected RandomAccessFile srLogRaf;

    protected File srIdxFile;

    protected RandomAccessFile srIdxRaf;

    protected long lastIndexed;

    public SearchResult(NASEnvironment environment, Heritrix3JobMonitor h3Job, String q, int searchResultNr) throws IOException {
        this.h3Job = h3Job;
        p = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
        //TODO Anyone want to take a stab at explaining this next line?
        m = p.matcher("42");
        srLogFile = new File(environment.tempPath, "crawllog-" + h3Job.jobId + "-" + searchResultNr + ".log");
        srLogRaf = new RandomAccessFile(srLogFile, "rw");
        srLogRaf.setLength(0);
        srIdxFile = new File(environment.tempPath, "crawllog-" + h3Job.jobId + "-" + searchResultNr + ".idx");
        srIdxRaf = new RandomAccessFile(srIdxFile, "rw");
        srIdxRaf.setLength(0);
        srIdxRaf.writeLong(0);
        lastIndexed = 0;
    }

    public synchronized void update() throws IOException {
        RandomAccessFile logRaf = new RandomAccessFile(h3Job.logFile, "r");
        logRaf.seek(lastIndexed);
        srLogRaf.seek(srLogRaf.length());
        srIdxRaf.seek(srIdxRaf.length());
        FileChannel logChannel = logRaf.getChannel();
        byte[] bytes = new byte[1024*1024];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        String tmpStr;
        //long index = lastIndex;
        long index = srLogRaf.length();
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
                            srLogRaf.write(bytes, mark, pos - mark);
                            index += pos - mark;
                            srIdxRaf.writeLong(index);
                        }
                        lastIndexed += pos - mark;
                        // next
                        mark = pos;
                        //index += pos - mark;
                        //lastIndex = index;
                    }
                } else {
                    b = false;
                }
            }
            byteBuffer.position(mark);
            byteBuffer.compact();
        }
        logRaf.close();
    }

    @Override
    public long getIndexSize() {
        return srIdxFile.length();
    }

    @Override
    public long getLastIndexed() {
        return srLogFile.length();
    }

    @Override
    public synchronized byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException {
        return StringIndexFile.readPage(srIdxRaf, srLogRaf, page, itemsPerPage, descending);
    }

    public synchronized void cleanup() {
        IOUtils.closeQuietly(srIdxRaf);
    }

}
