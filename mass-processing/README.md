# Mass Processing

This module is an experimental attempt to replace the Netarchive Distributed Processing with Hadoop Map-Reduce.

First, I added an package `dk.netarkivet.common.distribute.hadoop` in the module `common-core`.

Here I defined the class `HadoopArcRepositoryClient`. This class implements the interface `ArcRepositoryClient`, and should thus be something that can be plugged into the NetarchiveSuite without many problems.

`ArcRepositoryClient` defines the batch method
```java
public BatchStatus batch(FileBatchJob job, String replicaId, String... args);
```
and I defined a new method more suitable for hadoop:
```java
    public HadoopBatchStatus hadoopBatch(org.apache.hadoop.mapreduce.Job job, String replicaId);
```

The difference in the parameters is slight. The only change is really that rather than submitting custom netarchive jobs, you submit standard hadoop mapreduce jobs.

## Mapreduce Jobs

In this module, I have provided examples of such hadoop jobs

### File Checksum Job

This is a job that checksums whole files.

To define such a job, use this java code

```java
Job job = Job.getInstance();
Configuration conf = job.getConfiguration();

job.setJobName(FileChecksumJob.class.getName());
job.setJarByClass(FileChecksumJob.class);

//Set up input format
Path inputDir = new Path(args[0]);
job.setInputFormatClass(WholeFileInputFormat.class);
WholeFileInputFormat.addInputPath(job, inputDir);
WholeFileInputFormat.setInputPathFilter(job, MyPathFilter.class);
WholeFileInputFormat.setInputDirRecursive(job, true);


job.setMapperClass(ChecksumMapper.class);
job.setMapOutputKeyClass(Text.class);
job.setMapOutputValueClass(Text.class);

job.setReducerClass(DuplicateReducer.class);
job.setOutputKeyClass(Text.class);
job.setOutputValueClass(Text.class);
```
And then submit it with
```java
HadoopBatchStatus result = client.hadoopBatch(job, null);
```  

Let us go through the different sections

#### Creating the job object

```java
Job job = Job.getInstance();
Configuration conf = job.getConfiguration();

job.setJobName(FileChecksumJob.class.getName());
job.setJarByClass(FileChecksumJob.class);
```

#### Input files

```java
Path inputDir = new Path(args[0]);
job.setInputFormatClass(WholeFileInputFormat.class);
WholeFileInputFormat.addInputPath(job, inputDir);
WholeFileInputFormat.setInputPathFilter(job, MyPathFilter.class);
WholeFileInputFormat.setInputDirRecursive(job, true);
```

1. We convert the input argument to an hadoop path
2. We configure the job to use whole files as input key/value pairs, by setting the `WholeFileInputFormat.class`
3. We configure the `WholeFileInputFormat.class` to use the `inputDir`.
4. We configure the `WholeFileInputFormat.class` to use the defined Path filter.
5. We configure the `WholeFileInputFormat.class` to transverse the input path recusively.

This also reveals an interesting feature of hadoop jobs. You do NOT input objects, but classes. Objects would be difficult to serialise and distribute to other machines. Classes are comparetively simple. So you tell the job to use the `MyPathFilter.class`. The job will then create a new instance of this class when needed (so there must be a no-args constructor). If the class extends `Configured`, the system will also call `setConf(Configuration)`, so the new instance have access to the configuration.

As a lot of this is based on static methods on Job and InputFormats, there can be issues when the same JVM wants to invoke two independent jobs concurrently. The easiest thing would probably be to separate them in different classloaders, but this remains a problem to be solved (for us; I am sure somebody have already solved it).


Here is the `WholeFileInputFormat.class` in all it's gory:
* It is a class that extends `FileInputFormat`.
* The key/value pairs are Path/ByteArray, which in effect will be `Text/BytesWritable`

 

```java
package dk.netarkivet.common.distribute.hadoop;

public class WholeFileInputFormat extends FileInputFormat<Text, BytesWritable> {

    public WholeFileInputFormat() {
    }

    @Override protected boolean isSplitable(JobContext context, Path filename) {
        return false;
    }

    @Override public RecordReader<Text, BytesWritable> createRecordReader(InputSplit split, TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new WholeFileRecordReader();
    }
    
    @Override public List<InputSplit> getSplits(JobContext job) throws IOException {
        return super.getSplits(job);
    }
}
```

The `WholeFileInputFormat.class` does not do much, as almost all the work have been delegated to the ``WholeFileRecordReader`.

Forgive me for taking a brief detour here, to explain Hadoop Input formats.

Hadoop Input formats have two major responsibilities.

1. Compute the file splits
2. Create the record iterators for each file split

A map-reduce job consists of roughly these phases

1. (ApplicationMaster) compute splits from input params (via the InputFormat)
2. (ApplicationMaster) create 1 Map Task per Split
3. (ApplicationMasger) submit all map tasks to cluster
4. (Map Task) Use the `InputFormat.createRecordReader(split)` method to create the key/value pairs to **map**
5. (Map Task) Compute 
    ```
    for each (in_key, in_value) in recordReader; do 
            out_pairs = map(in_key, in_value); 
            save(out_pairs); 
    done
    ```
6. (ApplicationMaster) create ShuffleTasks to sort the output of all map tasks. This allows the job to group out_pairs by key
7. (Shuffle Task) sort pairs from map output files
8. (ApplicationMaster) Create a fixed (from config, default 1) number of reduce tasks
9. (ApplicationMaster) assign each group (grouped by key) of map output pairs to one reducer

10. (ApplicationMaster) start Reducers
11. (Reduce Tasks) Compute
   ```
    for each (in_key, in_values) in groups; do
        out_pairs = map(in_key, in_values); 
        save(out_pairs); 
    done
   ```

So, `InputFormat` is both the class that computes the `Splits`, i.e. the chunk that each map job will take as input, and the clas that generates the iterator that splits a `Split` into key/value pairs.

As the `WholeFileInputFormat` extends `FileInputFormat` and defines `isSplittable()` to be `false`, each `Split` is exactly one file.

The purpose of `WholeFileRecordReader` is then to generate the single key/value pair that represents the file. 

This sample implementation reads the file into memory (restriction on files > 2GB, TODO), and returns a pair of `(path,bytes[])` 
   

```java
public class WholeFileRecordReader extends RecordReader<Text, BytesWritable> {
    private Path path;
    private int length;

    private Configuration conf;

    private boolean processed = false;
    private BytesWritable value = new BytesWritable();
    private Text key;


    @Override public void initialize(InputSplit split, TaskAttemptContext context)
            throws IOException, InterruptedException {
        this.conf = context.getConfiguration();
        if (split instanceof FileSplit) {
            FileSplit fileSplit = (FileSplit) split;
            path = fileSplit.getPath();
            if (fileSplit.getLength() > Integer.MAX_VALUE){
                throw new IOException("File '"+fileSplit.getPath()+"' size "+fileSplit.getLength()+" greater than max value "+Integer.MAX_VALUE);
            } else {
                length = (int) fileSplit.getLength();
            }

        } else {
            throw new IOException("Not a file split");
        }
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        if (!processed) {
            key = new Text(path.toString());

            try(FileSystem fs = path.getFileSystem(conf);
                    FSDataInputStream in = fs.open(path);) {
                value.setCapacity(length);
                IOUtils.readFully(in, value.getBytes());
            }
            processed = true;
            return true;
        } else {
            key = null;
            value = null;
            return false;
        }
    }


    @Override public Text getCurrentKey() throws IOException, InterruptedException {
        return key;
    }

    @Override public BytesWritable getCurrentValue() throws IOException, InterruptedException {
        return value;
    }

    @Override public float getProgress() throws IOException, InterruptedException {
        return processed ? 1.0f : 0.0f;
    }

    @Override public void close() throws IOException {

    }
}
```

And finally, we come to the PathFilter.
The PathFilter is a simple class, that is invoked when `FileInputFormat` (and thus `WholeFileInputFormat`) traverses the input path(s) recursively.

Notice that we have to accept all directories, as otherwise we break the recursiveness of the traversal.

```java
public static class MyPathFilter extends Configured implements PathFilter {

    @Override public boolean accept(Path path) {
        String name = path.getName();

        try {
            FileSystem fileSystem = FileSystem.get(getConf());

            if (fileSystem.isDirectory(path)) {
                return true;
            }
            return name.endsWith(".arc");
            //                return name.endsWith(".warc") || name.endsWith(".warc.gz") || name.endsWith(".arc") || name.endsWith(".arc.gz");
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
    }
}
```


#### Map Phase

Now we come to the setup of the map-steps
```java
job.setMapperClass(ChecksumMapper.class);
job.setMapOutputKeyClass(Text.class);
job.setMapOutputValueClass(Text.class);
```

We define the Mapper class, so the job can create new Mapper instances as needed, and we define the Map output key/value types, which will also be the input types for the shuffle and reduce steps.

As the Mapper maps files `(Path/Bytes)` to `(Checksum/Path)`, the output types are `(Text/Text)`

```java
 public class ChecksumMapper extends Mapper<Text, BytesWritable, Text, Text> {

    private static final Logger log = LoggerFactory.getLogger(ChecksumMapper.class);

    @Override protected void map(Text key, BytesWritable value, Context context)
            throws IOException, InterruptedException {
        log.info("Working on file {}", key.toString());
        Text out_key = new Text(DigestUtils.md5Hex(value.getBytes()));
        Text out_value = new Text(key.toString());
        context.write(out_key, out_value);
    }
}
```

#### Reduce Phase 

And the Reduce phase. 
```java
job.setReducerClass(DuplicateReducer.class);
job.setOutputKeyClass(Text.class);
job.setOutputValueClass(Text.class);
```

The purpose of this job was to find duplicated files, we used the checksum as the output key from the Map phase. This shuffler will thus group the pairs on checksum, and the reducer is thus fed one checksum and a Iterable of paths.

If the iterable of paths contain more than one path, we join the paths and output them. Otherwise, we do not. 

```java
public class DuplicateReducer extends Reducer<Text, Text, Text, Text> {
    private static final Logger log = LoggerFactory.getLogger(FileChecksumJob.DuplicateReducer.class);

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        //DANGER WILL ROBINSON, THE ITERABLE IS AN ITERATOR AND CAN ONLY BE READ ONCE
        List<Text> list = new ArrayList<>();
        Iterator<Text> iterator = values.iterator();
        while (iterator.hasNext()) {
            Text next = iterator.next();
            list.add(new Text(next.toString()));
        }

        log.info("Found {} files {} with checksum {}", list.size(), list, key.toString());
        if (list.size() > 1) {
            String paths = String.join(
                    File.pathSeparator,
                    list.stream().map(Text::toString).collect(Collectors.toList()));
            context.write(key, new Text(paths));
        }

    }
}
```

And this was the whole of the File Checksum Job. 

### Hadoop Batch method implementation

Now let us look at how 
```java
HadoopBatchStatus result = client.hadoopBatch(job, null);
``` 
have been implemented

```java
public HadoopBatchStatus hadoopBatch(Job job,  String replicaId) {
    //We want to do as much work as possible and return results for the successes, along with a list of the files that
    //failed to process.

    job.setOutputFormatClass(TextOutputFormat.class);
    Path outputDir = new Path("target/temp" + new Date().getTime());
    TextOutputFormat.setOutputPath(job, outputDir);

    boolean success;
    try {
        success = job.waitForCompletion(true);
    } catch (IOException | InterruptedException | ClassNotFoundException e) {
        throw new IOFailure("message", e);
    }
    int num_files = Integer.parseInt(job.getConfiguration().get(FileInputFormat.NUM_INPUT_FILES));

    try {
        Arrays.stream(job.getTaskReports(TaskType.MAP))
                .filter(task -> task.getCurrentStatus() == TIPStatus.FAILED)
                .map(TaskReport::getDiagnostics)
                .forEachOrdered(errors -> log.error(Arrays.asList(errors).toString()));
    } catch (IOException | InterruptedException e) {
        throw new IOFailure("message",e);
    }
    //TODO info about failed tasks

    FileSystem srcFS;
    try {
        srcFS = FileSystem.get(job.getConfiguration());
    } catch (IOException e){
        throw new IOFailure("message", e);
    }
    RemoteFile resultFile = getResultFile(outputDir, srcFS);

    return new HadoopBatchStatus(
            success,
            num_files,
            null,
            replicaId,
            resultFile,
            null);
}
```

First, we configure some additional things for the job (which could also just have been done for `hadoopBatch(...)` was invoked)

#### Output format and path

```java
job.setOutputFormatClass(TextOutputFormat.class);
Path outputDir = new Path("target/temp" + new Date().getTime());
TextOutputFormat.setOutputPath(job, outputDir);
```

We set the `OutputFormatClass` to `TextOutputFormat`. This means that the output of the reducers are written to a text file.

We define the output directory to a semi-random folder that does not already exist, and configure the job with this. This allows us to read the results later on, when we return a resulting `RemoteFile`;

#### Job started
```java
boolean success;
try {
    success = job.waitForCompletion(true);
} catch (IOException | InterruptedException | ClassNotFoundException e) {
    throw new IOFailure("message", e);
}
```

Then we start the job and waits for it to complete.

#### Hadoop Batch Status

When the job have completed, we start to collect information for the `HadoopBatchStatus` object, which we must return.

First are the number of input files
```java
int num_files = Integer.parseInt(job.getConfiguration().get(FileInputFormat.NUM_INPUT_FILES));
```
This is something that we can get from the job configuration.
  
Then something about the jobs that failed. This is still Work-In-Progress and actually just logs the errors on ERROR level, rather than collecting them

```java
try {
    Arrays.stream(job.getTaskReports(TaskType.MAP))
            .filter(task -> task.getCurrentStatus() == TIPStatus.FAILED)
            .map(TaskReport::getDiagnostics)
            .forEachOrdered(errors -> log.error(Arrays.asList(errors).toString()));
} catch (IOException | InterruptedException e) {
    throw new IOFailure("message",e);
}
```

Then the resulting `RemoteFile` is loaded
```java
FileSystem srcFS;
try {
    srcFS = FileSystem.get(job.getConfiguration());
} catch (IOException e){
    throw new IOFailure("message", e);
}
RemoteFile resultFile = new HadoopRemoteFile(outputDir, srcFS);
``` 

This seems simple, because all the work is delegated to the `HadoopRemoteFile` class

```java
package dk.netarkivet.common.distribute.hadoop;

public class HadoopRemoteFile implements RemoteFile, Closeable {

    private final Path outputDir;
    private final FileSystem srcFS;

    public HadoopRemoteFile(Path outputDir, FileSystem srcFS) {
        this.outputDir = outputDir;
        this.srcFS = srcFS;
    }

    @Override public void copyTo(File destFile) {
        try (InputStream inputStream = getInputStream()) {
            try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
                org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream);
            }
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }

    }

    @Override public void appendTo(OutputStream out) {
        try {
            RemoteIterator<LocatedFileStatus> files = srcFS
                    .listFiles(outputDir, true);
            while (files.hasNext()) {
                LocatedFileStatus next = files.next();
                if (next.isFile()) {
                    try (InputStream inpustream = srcFS.open(next.getPath())) {
                        IOUtils.copyBytes(inpustream, out, 4096, false);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
    }

    @Override public InputStream getInputStream() {

        try {

            RemoteIterator<LocatedFileStatus> files = srcFS
                    .listFiles(outputDir, true);

            ArrayList<LocatedFileStatus> filesAsList = new ArrayList<>();
            while (files.hasNext()) {
                LocatedFileStatus next = files.next();
                if (next.isFile()) {
                    filesAsList.add(next);
                }
            }
            Iterator<FSDataInputStream> inputstreamIterator = filesAsList.stream()
                    .map(file -> {
                        try {
                            return srcFS.open(file.getPath());
                        } catch (IOException e) {
                            throw new IOFailure("message", e);
                        }
                    }).iterator();

            return new SequenceInputStream(inputstreamIterator);
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
    }

    @Override public String getName() {
        return outputDir.getName();
    }

    @Override public String getChecksum() {
        return null;
    }

    @Override public void cleanup() {
        try {
            close();
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
    }

    @Override public void close() throws IOException {
        srcFS.delete(outputDir, true);
    }

    @Override public long getSize() {
        try {
            if (!srcFS.exists(outputDir)){
                return 0;
            }
            return Arrays.stream(srcFS.listStatus(outputDir)).mapToLong(FileStatus::getLen).sum();
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }

    }

    @Override public String toString() {
        return "HadoopRemoteFile{" +
                "outputDir=" + outputDir + ", " +
                "name=" + getName() + ", " +
                "size=" + getSize() +
                '}';
    }
}
```

This class does two or three things.

1. It opens and reads a file from a HDFS system.
2. It allows you to open multiple files as one inputstream
3. It deletes the result-dir when closed

Why should it read multiple files, you might ask? Well, the number of result files from a Map/Reduce job is equal to the number of reducer tasks. Each reducer have been assigned a number of whole groups (see above), so there will have been no overlap. If multiple reducer tasks have run, each will produce an output-file from the key/value-list pairs it received.

So I created (brutally copied from java standard libary) `SequenceInputStream`. The reason I could not use the standard implementation is that this was written to use `Enumeration`, instead of `Iterator`. `Enumeration` is a forgotten interface and surprisingly difficult to use. Note that the javadoc for `Enumeration` says 
```
* NOTE: The functionality of this interface is duplicated by the Iterator
* interface.  In addition, Iterator adds an optional remove operation, and
* has shorter method names.  New implementations should consider using
* Iterator in preference to Enumeration.
```
But for some (insane?) reason, java have never created a new implementation of SequenceInputStream. So I have to roll my own.

```java
/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package dk.netarkivet.common.distribute.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A <code>SequenceInputStream</code> represents
 * the logical concatenation of other input
 * streams. It starts out with an ordered
 * collection of input streams and reads from
 * the first one until end of file is reached,
 * whereupon it reads from the second one,
 * and so on, until end of file is reached
 * on the last of the contained input streams.
 *
 * @author  Author van Hoff
 * @since   JDK1.0
 */
public
class SequenceInputStream extends InputStream {
    Iterator<? extends InputStream> e;
    InputStream in;

    /**
     * Initializes a newly created <code>SequenceInputStream</code>
     * by remembering the argument, which must
     * be an <code>Enumeration</code>  that produces
     * objects whose run-time type is <code>InputStream</code>.
     * The input streams that are  produced by
     * the enumeration will be read, in order,
     * to provide the bytes to be read  from this
     * <code>SequenceInputStream</code>. After
     * each input stream from the enumeration
     * is exhausted, it is closed by calling its
     * <code>close</code> method.
     *
     * @param   e   an enumeration of input streams.
     * @see     java.util.Enumeration
     */
    public SequenceInputStream(Iterator<? extends InputStream> e) {
        this.e = e;
        try {
            nextStream();
        } catch (IOException ex) {
            // This should never happen
            throw new Error("panic");
        }
    }

    /**
     * Initializes a newly
     * created <code>SequenceInputStream</code>
     *
     * @param   streams   the input streams to read.
     */
    public SequenceInputStream(InputStream... streams) {
        e = Arrays.stream(streams).iterator();
        try {
            nextStream();
        } catch (IOException ex) {
            // This should never happen
            throw new Error("panic");
        }
    }

    /**
     *  Continues reading in the next stream if an EOF is reached.
     */
    final void nextStream() throws IOException {
        if (in != null) {
            in.close();
        }

        if (e.hasNext()) {
            in = e.next();
            if (in == null)
                throw new NullPointerException();
        }
        else in = null;

    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from the current underlying input stream without
     * blocking by the next invocation of a method for the current
     * underlying input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * This method simply calls {@code available} of the current underlying
     * input stream and returns the result.
     *
     * @return an estimate of the number of bytes that can be read (or
     *         skipped over) from the current underlying input stream
     *         without blocking or {@code 0} if this input stream
     *         has been closed by invoking its {@link #close()} method
     * @exception  IOException  if an I/O error occurs.
     *
     * @since   JDK1.1
     */
    public int available() throws IOException {
        if (in == null) {
            return 0; // no way to signal EOF from available()
        }
        return in.available();
    }

    /**
     * Reads the next byte of data from this input stream. The byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the
     * stream has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of the
     * stream is detected, or an exception is thrown.
     * <p>
     * This method
     * tries to read one character from the current substream. If it
     * reaches the end of the stream, it calls the <code>close</code>
     * method of the current substream and begins reading from the next
     * substream.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read() throws IOException {
        while (in != null) {
            int c = in.read();
            if (c != -1) {
                return c;
            }
            nextStream();
        }
        return -1;
    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes.  If <code>len</code> is not zero, the method
     * blocks until at least 1 byte of input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     * <p>
     * The <code>read</code> method of <code>SequenceInputStream</code>
     * tries to read the data from the current substream. If it fails to
     * read any characters because the substream has reached the end of
     * the stream, it calls the <code>close</code> method of the current
     * substream and begins reading from the next substream.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes read.
     * @return     int   the number of bytes read.
     * @exception  NullPointerException If <code>b</code> is <code>null</code>.
     * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
     * <code>len</code> is negative, or <code>len</code> is greater than
     * <code>b.length - off</code>
     * @exception  IOException  if an I/O error occurs.
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (in == null) {
            return -1;
        } else if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        do {
            int n = in.read(b, off, len);
            if (n > 0) {
                return n;
            }
            nextStream();
        } while (in != null);
        return -1;
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * A closed <code>SequenceInputStream</code>
     * cannot  perform input operations and cannot
     * be reopened.
     * <p>
     * If this stream was created
     * from an enumeration, all remaining elements
     * are requested from the enumeration and closed
     * before the <code>close</code> method returns.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        do {
            nextStream();
        } while (in != null);
    }
}
```


#### Hadoop Batch Status

Finally, we create a new Hadoop Batch Status

```java

return new HadoopBatchStatus(
        success,
        num_files,
        null,
        replicaId,
        resultFile,
        null);
```

The `HadoopBatchStatus` class have been modelled to resemble the existing `BatchStatus` class

```java
public class BatchStatus {

    /** The total number of files processed so far. */
    private final int noOfFilesProcessed;
    /** A list of files that the batch job could not process. */
    private final Collection<File> filesFailed;
    /** The application ID identifying the bitarchive, that run this batch job. */
    private final String bitArchiveAppId;
    /** The file containing the result of the batch job. */
    private RemoteFile resultFile;

    /** A list of exceptions caught during the execution of the batchJob. */
    private final List<ExceptionOccurrence> exceptions;

    ...
}
```

```java
public class HadoopBatchStatus {

    private boolean success;

    /** The total number of files processed so far. */
    private final int noOfFilesProcessed;
    
    /** A list of files that the batch job could not process. TODO not yet possible*/
    private final Collection<URI> filesFailed;
    /**
     * The application ID identifying the bitarchive, that run this batch job. */
    private final String bitArchiveAppId;
    
    /** The file containing the result of the batch job. */
    private RemoteFile resultFile;

    /** A list of exceptions caught during the execution of the batchJob. */
    private final List<Exception> exceptions;

...
}
```

The major change is the `filesFailed` collection. Because HDFS files and other remote files cannot be expressed as a protocol-less java `File` object, I had to change the type here. `URI` is a quite general type that anything resource with a protocol can be expressed as, including both local `File`s and Hadoop `Path`s.

However, it have turned out to be surprisingly hard to get information about exactly which files in a job that failed. Due to the levels of indirection inherent in `Inputdir->Recursive traversal->InputSplits->Distribution as Tasks->RecordReader`, I can get a lot of information about which Tasks that failed, but no programmatically readable information about which Input split was assigned to this task.

Everything I need to know is of course logged, so it is not like the information is not there, but it is not readily available in a programmatic fashion.



## Testing a Hadoop Job

I created the module `mass-processing` to be able to test hadoop jobs.

The issue here is that you need to provide the hadoop system with some jar and/or class files. The easiest way to do this is to create a all-in-one jar. So I added the assembly plugin to the `pom.xml` for `mass-processing`

```xml
<build>
    <plugins>
        <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>single</goal>
                </goals>
              </execution>
            </executions>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

In order to get better log statements, I added this `log4j.xml` file to `src/main/resources`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">

<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
  <appender name="console" class="org.apache.log4j.ConsoleAppender">
    <param name="Target" value="System.out"/>
    <layout class="org.apache.log4j.PatternLayout">
      <param name="ConversionPattern" value="%5p [%t] (%F:%L) - %m%n"/>
    </layout>
  </appender>

  <root>
    <priority value="debug"/>
    <appender-ref ref="console"/>
  </root>

</log4j:configuration>
```

### Running on the Narcana cluster

I created a user `dkm_eld` (passive-aggressive handling of not being able to get a name for the project other than something about dkm and it being handled by Eld Zierau)

To run a job, such as the File Checksum Job, I then execute the command from the `runBatchProcessingOnNarcana.sh` script
```bash
#!/usr/bin/env bash

set -e
set -x

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

cd "$SCRIPT_DIR"

mvn -DskipTests -Phortonworks -Psbprojects-nexus install -pl mass-processing -am

scp ~/Projects/netarchivesuite/mass-processing/target/mass-processing-*-jar-with-dependencies.jar dkm_eld@narcana-webdanica01:.

ssh dkm_eld@narcana-webdanica01 \
    yarn jar mass-processing-5.6-SNAPSHOT-jar-with-dependencies.jar dk.netarkivet.common.utils.archive.FileChecksumJob hdfs://HDFS/user/dkm_eld/netarchivesuite/

```
Previously, I copied the netarchive codebase to HDFS so we would have some (arc/warc) files to work on.



