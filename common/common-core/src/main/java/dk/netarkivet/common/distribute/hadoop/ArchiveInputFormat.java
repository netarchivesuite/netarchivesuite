package dk.netarkivet.common.distribute.hadoop;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

public class  ArchiveInputFormat extends FileInputFormat<Text, ArchiveRecordBase> {

    private static final Logger log = LoggerFactory.getLogger(ArchiveInputFormat.class);

    public static class ArchiveRecordReader extends RecordReader<Text, ArchiveRecordBase> {

        private static final Logger log = LoggerFactory.getLogger(ArchiveRecordReader.class);

        private Path archivePath;
        private DataInputStream inputStream;

        private FileSystem fileSystem;
        private ArchiveReader archiveReader;
        private Iterator<ArchiveRecord> iterator;

        private ArchiveRecord currentRecord;
        private String key;
        private long length;
        private long start;
        private InputSplit split;
        private TaskAttemptContext context;

        @Override public void initialize(InputSplit split, TaskAttemptContext context)
                throws IOException, InterruptedException {
            this.split = split;
            this.context = context;

            Configuration conf = context.getConfiguration();

            this.length = split.getLength();
            if (split instanceof FileSplit) {
                FileSplit fileSplit = (FileSplit) split;
                this.archivePath = fileSplit.getPath();
                this.start = fileSplit.getStart();
            }

            log.debug("Starting to read records from {}",split);
            this.fileSystem = archivePath.getFileSystem(conf);
            this.inputStream = fileSystem.open(archivePath);
            ((FSDataInputStream) inputStream).seek(start);
            this.archiveReader = ArchiveReaderFactory.get(archivePath.toString(), inputStream, false);

            archiveReader.setStrict(false);
            this.iterator = archiveReader.iterator();
            context.setStatus(split.toString());
            Thread.currentThread().setName(split.toString());
        }

        @Override public boolean nextKeyValue() throws IOException, InterruptedException {

            IOUtils.closeQuietly(currentRecord);

            boolean hasNext = iterator.hasNext();
            if (hasNext){
                long position = currentRecord != null ? currentRecord.getPosition() : 0;
                try {
                    currentRecord = iterator.next();
                } catch (RuntimeException e){
                    throw new RuntimeException("Failed to read record at "+position,e);
                }
                if (currentRecord == null) {
                    return false;
                }
                if (position - start > length){
                    log.debug("This record is starts {} beyound the length {} of this split, so ignore it", position, split.getLength());
                    return false;
                }
                key = archivePath.toString() + ":" + position + ":" + currentRecord.getHeader()
                        .getContentLength();
                Thread.currentThread().setName(key);
                context.setStatus(key);


//                TODO logging and failed files

            } else {
                log.debug("No more records in {}",split);
            }
            return hasNext;
        }

        @Override public Text getCurrentKey() throws IOException, InterruptedException {
            return new Text(key);
        }

        @Override public ArchiveRecordBase getCurrentValue() throws IOException, InterruptedException {
            return ArchiveRecordBase.wrapArchiveRecord(currentRecord);
        }

        @Override public float getProgress() throws IOException, InterruptedException {
            if (currentRecord != null) {
                return (currentRecord.getPosition() - start + 0.0f) / length;
            } else {
                return 0.0f;
            }
        }

        @Override public void close() throws IOException {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(archiveReader);
            IOUtils.closeQuietly(fileSystem);
            IOUtils.closeQuietly(currentRecord);
        }
    }

    @Override protected boolean isSplitable(JobContext context, Path filename) {
        return false;
    }

    @Override public RecordReader<Text, ArchiveRecordBase> createRecordReader(InputSplit split,
            TaskAttemptContext context) throws IOException, InterruptedException {
        context.setStatus(split.toString());
        return new ArchiveRecordReader();
    }
}
