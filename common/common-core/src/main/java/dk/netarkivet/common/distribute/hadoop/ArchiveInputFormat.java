package dk.netarkivet.common.distribute.hadoop;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
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

import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

public class  ArchiveInputFormat extends FileInputFormat<Text, ArchiveRecordBase> {

    public static class ArchiveRecordReader extends RecordReader<Text, ArchiveRecordBase> {

        private Path archivePath;
        private DataInputStream inputStream;

        private FileSystem fileSystem;
        private ArchiveReader archiveReader;
        private Iterator<ArchiveRecord> iterator;

        private ArchiveRecord currentRecord;
        private long length;

        @Override public void initialize(InputSplit split, TaskAttemptContext context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();

            long start = 0;
            if (split instanceof FileSplit) {
                FileSplit fileSplit = (FileSplit) split;
                archivePath = fileSplit.getPath();
                start = fileSplit.getStart();
                length = fileSplit.getLength();
            }

            fileSystem = archivePath.getFileSystem(conf);


            inputStream = fileSystem.open(archivePath);
            ((FSDataInputStream) inputStream).seek(start);


            archiveReader = ArchiveReaderFactory.get(archivePath.toString(), inputStream, false);

            iterator = archiveReader.iterator();
        }

        @Override public boolean nextKeyValue() throws IOException, InterruptedException {

            IOUtils.closeQuietly(currentRecord);

            boolean hasNext = iterator.hasNext();
            if (hasNext){
                currentRecord = iterator.next();

                if (currentRecord.getPosition() > length){
                    //This record is beyound the length of the split, so stop here
                    return false;
                }

            }
            return hasNext;
        }

        @Override public Text getCurrentKey() throws IOException, InterruptedException {
            return new Text(archivePath.toString() + ":"+currentRecord.getPosition());
        }

        @Override public ArchiveRecordBase getCurrentValue() throws IOException, InterruptedException {
            return ArchiveRecordBase.wrapArchiveRecord(currentRecord);
        }

        @Override public float getProgress() throws IOException, InterruptedException {
            return (currentRecord.getPosition() + 0.0f )/ fileSystem.getFileStatus(archivePath).getLen();
        }

        @Override public void close() throws IOException {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(archiveReader);
            IOUtils.closeQuietly(fileSystem);
            IOUtils.closeQuietly(currentRecord);
        }
    }

    @Override protected FileSplit makeSplit(Path file, long start, long length, String[] hosts) {
        return super.makeSplit(file, start, length, hosts);
    }

    @Override public List<InputSplit> getSplits(JobContext job) throws IOException {
        return super.getSplits(job);
    }

    @Override protected boolean isSplitable(JobContext context, Path filename) {
        return false;
    }


    @Override public RecordReader<Text, ArchiveRecordBase> createRecordReader(InputSplit split,
            TaskAttemptContext context) throws IOException, InterruptedException {
        return new ArchiveRecordReader();
    }
}
