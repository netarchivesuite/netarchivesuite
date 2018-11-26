# Archive Checksum Job

Note, read the `README.md` file first, as I do not want to repeat everything about hadoop jobs again.

I created the job `ArchiveChecksumJob`. It mirrors `FileChecksumJob` closely, in that it takes an input dir as input, which it traverses recursively, and checksums all files found. It then groups the records on checksum and outputs them where 2 or more records share a checksum.

`ArchiveChecksumJob` does exactly this, except that it operates on the individual records in the arc/warc files, rather than on whole files.


As before, the job creation, and invocation is pretty standard
```java


Job job = Job.getInstance();
Configuration conf = job.getConfiguration();

job.setJobName(ArchiveChecksumJob.class.getName());
job.setJarByClass(ArchiveChecksumJob.class);

//Do not retry failures
conf.set(MRJobConfig.MAP_MAX_ATTEMPTS, "1");

//Job is not failed due to failures
conf.set(MRJobConfig.MAP_FAILURES_MAX_PERCENT, "100");

//Set up input format
Path inputDir = new Path(args[0]);
job.setInputFormatClass(ArchiveInputFormat.class);
//Filter for filenames
try {
    ArchiveInputFormat.addInputPath(job, inputDir);
} catch (IOException e) {
    throw new IOFailure("message", e);
}
ArchiveInputFormat.setInputPathFilter(job, MyPathFilter.class);
ArchiveInputFormat.setInputDirRecursive(job, true);

job.setMapperClass(ChecksumMapper.class);
job.setMapOutputKeyClass(Text.class);
job.setMapOutputValueClass(Text.class);

job.setReducerClass(DuplicateReducer.class);
job.setOutputKeyClass(Text.class);
job.setOutputValueClass(Text.class);

HadoopArcRepositoryClient client = new HadoopArcRepositoryClient();
HadoopBatchStatus result = client.hadoopBatch(job, null);

System.out.println(result);
try (InputStream resultFile = result.getResultFile().getInputStream()) {
//            IOUtils.copy(resultFile, System.out);
} finally {
    result.getResultFile().cleanup();
}

return result.isSuccess() ? 0 : 1;
``` 

The differences here are 
1. A few Configuration flags have been set
2. The Inputformat is `ArchiveInputFormat`

The config flags in question are

```java
//Do not retry failures
conf.set(MRJobConfig.MAP_MAX_ATTEMPTS, "1");

//Job is not failed due to failures
conf.set(MRJobConfig.MAP_FAILURES_MAX_PERCENT, "100");
```
I hope the comments are sufficient to explain their reason


The interesting this here is the `ArchiveInputFormat`

```java
public class  ArchiveInputFormat extends FileInputFormat<Text, ArchiveRecordBase> {

    private static final Logger log = LoggerFactory.getLogger(ArchiveInputFormat.class);

    @Override protected boolean isSplitable(JobContext context, Path filename) {
        return false;
    }

    @Override public RecordReader<Text, ArchiveRecordBase> createRecordReader(InputSplit split,
            TaskAttemptContext context) throws IOException, InterruptedException {
        context.setStatus(split.toString());
        return new ArchiveRecordReader();
    }
}
```

No so interesting after all? Well, it does highlight an interesting thing about the mapreduce pairs and serialisation. So a brief detour.

Hadoop is based around the idea of moving computation to the data. This means that a lot of classes need to be serialised and deserialised. But somethings data needs to move to the computation. If we remember the mapreduce process from before, this should be clear

1. Inputformat creates splits
2. Splits are serialized (to disk) and send to map tasks
3. Map task deserialize the split and reads it with (new instanse of) record reader
4. Map task outputs key/value pairs, which are serialised (to disk)
5. Shuffler deserialises map outputs key/value pairs (from disk) and sorts them (serialised back to disk)
6. Reducer deserialize the shuffler output (thus map output key/value) pairs and works on them
7. Reducer outputs key/value pairs, which are serialized to the configured OutputFormat

When we note that the way to serialise objects in hadoop is to implement the `Writable` interface.

```java
public interface Writable {
  /** 
   * Serialize the fields of this object to <code>out</code>.
   * 
   * @param out <code>DataOuput</code> to serialize this object into.
   * @throws IOException
   */
  void write(DataOutput out) throws IOException;

  /** 
   * Deserialize the fields of this object from <code>in</code>.  
   * 
   * <p>For efficiency, implementations should attempt to re-use storage in the 
   * existing object where possible.</p>
   * 
   * @param in <code>DataInput</code> to deseriablize this object from.
   * @throws IOException
   */
  void readFields(DataInput in) throws IOException;
}
```

As the list above shows, the Splits must be Writable as they are serialised when shipped to the map task.

However, the key/value pairs that the InputFormat produces (via RecordReader) does not, as they are never serialised.

The mapper is initialized with four types, Input key/value and output key/value. The input key/value should be the RecordReader output types, and they do NOT need to be Writable.

The output types of the Mapper does need to be writable, however.

The output types of the mapper will also be the input types of the Reducer. 

The output types of the Reducer will be written to disk and must thus be Writable.

This is important, as implementing the `Writable` can be bothersome for complex types. Hadoop provides standard implementation of the interface for simple types

* Text for String
* IntWritable for Integers
* ... And so on.

There are a number of interesting data types and utility classes, which I could probably have used to some effect, if I had noticed them earlier, like `MD5Hash`, `ObjectWritable`, `DefaultStringifier` and `WritableUtils`


So why this detour? Well, the thing is that we already have a good class to represent a record in a arc/warc file, namely `ArchiveRecordBase`

```java
public abstract class ArchiveRecordBase {

    /** Is this record from an ARC file. */
    public boolean bIsArc;

    /** Is this record from a WARC file. */
    public boolean bIsWarc;

    /**
     * Return the wrapped Heritrix archive header
     *
     * @return the wrapped Heritrix archive header
     */
    public abstract ArchiveHeaderBase getHeader();

    /**
     * Return the payload input stream.
     *
     * @return the payload input stream
     */
    public abstract InputStream getInputStream();
...
}
```

This obviously does NOT implement `Writable` and making it do so, especially with the difficulties from the `ArchiveHeaderBase` and `Inputstream` is no small task.

However, the output types of InputFormats (really, RecordReader) do Not need to be writable, only the splits do.


So we can create a `ArchiveRecordReader` like this:  

```java
public class ArchiveRecordReader extends RecordReader<Text, ArchiveRecordBase> {

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

        length = split.getLength();
        if (split instanceof FileSplit) {
            FileSplit fileSplit = (FileSplit) split;
            archivePath = fileSplit.getPath();
            start = fileSplit.getStart();
        }

        log.debug("Starting to read records from {}",split);
        fileSystem = archivePath.getFileSystem(conf);
        inputStream = fileSystem.open(archivePath);
        ((FSDataInputStream) inputStream).seek(start);
        archiveReader = ArchiveReaderFactory.get(archivePath.toString(), inputStream, false);

        archiveReader.setStrict(false);
        iterator = archiveReader.iterator();
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
```

Let us go through this in steps

#### ArchiveRecordReader initialise

```java
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
```

This is the constructor-like method. As the class is created from a class, it must have a no-args constructor, so initialise takes the place of the argumented contructor.

1. First we set a few variables (split and context)
    ```java
    this.split = split;
    this.context = context;
    Configuration conf = context.getConfiguration();
    ```
2. Then we examine the given split
    ```java
    this.length = split.getLength();
    if (split instanceof FileSplit) {
        FileSplit fileSplit = (FileSplit) split;
        this.archivePath = fileSplit.getPath();
        this.start = fileSplit.getStart();
    }
    ```
    1. We assume it is a fileSplit (which it will always be with the kind of jobs we run), so we can cast it
    2. We get the path, test start offset and the length. Note, `ArchiveInputFormat` stated that the files are not splittable, so the input split will always be a whole file. This was just some code for WHEN we tried to let the system split the input files in equal-sized chunks.
3. Then we open the file, skips to the right start offset and feeds it to `ArchiveReaderFactory`
    ```java
    this.fileSystem = archivePath.getFileSystem(conf);
    this.inputStream = fileSystem.open(archivePath);
    ((FSDataInputStream) inputStream).seek(start);
    this.archiveReader = ArchiveReaderFactory.get(archivePath.toString(), inputStream, false);
    ```
4. And finally, we create the record iterator from the archiveReader and sets some status messages
    ```java
    archiveReader.setStrict(false);
    this.iterator = archiveReader.iterator();

    context.setStatus(split.toString());
    Thread.currentThread().setName(split.toString());
    ```
    
#### ArchiveRecordReader nextKeyValue

An RecordReader is like an Iterator, but not quite. I really do not know why Hadoop did not want to follow the standard interface, but whatever.

RecordReaders have three methods, `nextKeyValue`, `getCurrentKey` and `getCurrentValue`. The `nextKeyValue` advances the iterator one step or returns false. Then `getCurrentKey` and `getCurrentValue` can be called as many times as needed without affecting the iterator.  


Here is the implementation of `nextKeyValue()`

```java
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
    } else { log.debug("No more records in {}",split);  }
    return hasNext;
}
```    

First we close the current Record. The `closeQuietly` method from IOUtils will NOT throw a nullpointer exception if the current Record is null.

`boolean hasNext = iterator.hasNext();` Then we check if there is another record in the arc/warc file.

`if (hasNext){` if yes, we have to handle it;
`} else { log.debug("No more records in {}",split); }` And if not, just log this state of affairs.

`long position = currentRecord != null ? currentRecord.getPosition() : 0;` We determine the position before reading the record. The position is (I think) the end of the record, so by determining the position of the previous record, we get the start of the about-to-be-read record.

```java
try {
    currentRecord = iterator.next();
} catch (RuntimeException e){
    throw new RuntimeException("Failed to read record at "+position,e);
}
```
Then we read the record from the iterator.

```java
if (currentRecord == null) {
    return false;
}
```
I do not know if it is possible for the iterator to return null, but I regard is as a End-of-file, if it happens.

```java
if (position - start > length){
    log.debug("This record is starts {} beyound the length {} of this split, so ignore it", position, split.getLength());
    return false;
}
```
If the start of the currently read record is beyound the length of the split (only relevant if the split is not the whole file), pretend that we never read it and return false.
 
 
```java
key = archivePath.toString() + ":" + position + ":" + currentRecord.getHeader()
        .getContentLength();

Thread.currentThread().setName(key);
context.setStatus(key);
```
And finally update the key and set some status'es (only relevant for logging)


#### ArchiveRecordReader, the rest

The remaining methods are simple and do not require further explanation.
```java
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
```

### ChecksumMapper with ArchiveInputFormat

The checksummapper is ever so slightly changed to deal with the new input-value in the form of `ArchiveRecordBase`

```java
public static class ChecksumMapper extends Mapper<Text, ArchiveRecordBase, Text, Text> {

    @Override protected void map(Text key, ArchiveRecordBase value, Context context)
            throws IOException, InterruptedException {
        try (InputStream in = value.getInputStream()) {
            String digest = MD5Hash.digest(in).toString();
            context.write(new Text(digest), key);
        }
    }
}
```

We would even have used `MD5Hash` as the new Output key, but I never tried that.

Everything else remained as it was in `FileChecksumJob`.

