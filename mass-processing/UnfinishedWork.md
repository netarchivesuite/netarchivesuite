# Unfinished Work

These are the major issues that still needs to be resolved.

## Collection of failed files

As noted in the explations, it is surprisingly difficult to get programmatic information about which splits that failed. 

The best I came up with was this

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
The exception messages thus collected can (if you include it) contain the file path, but you will have to parse text to get it. This does NOT seem ideal.

I also thought about forcing each mapper to output a special key/value pair just with the file path. If a Mapper task fails, all the output pairs from the task is discarded. So this pair would only flow onto the reducer if the mapper did not crash on any of the records in the split.

That design runs into problems about forcing specific types on mapOutput keys/values. There is really no room for the path, if the mapper outputs Integer/Integer. And requiring that your map output keys are always Text/Text is restrictive for a number of workflows.

Secondly, the reducer needs to be able to handle this extra output correctly, i.e. not as the other outputs from the map task. 

All in all, this feels like a bad way to get the failed files.


## Invocation from NetarchiveSuite

I have only tested my hadoop jobs by invoking them from the command line. While they do use a lot of NetarchiveSuite code, they have NOT been invoked by a Netarchive Process.

I was really not entirely sure on how to even run a NetarchiveSuite instance, let alone how to configure it to use another `ArcRepositoryClient`.

As I have mentioned before, the hadoop way of configuring a job by using Static methods might turn out to be quite difficult to make work in a concurrent NetarchiveSuite webservice, and I expect some problems to arise. Separating class-loaders seems like the better strategy.

Furthermore, to correctly invoke a hadoop job, the process must be run on a system with at least the hadoop config files present (i.e. with the variable `HADOOP_CONF_DIR='/usr/hdp/2.6.0.3-8/hadoop/conf'`), and prefably the hadoop clients installed.

My tests have been conducted on `dkm_eld@narcana-webdanica01.statsbiblioteket.dk`, which is hadoop access node in a totally unsecured cluster. If the cluster was secured, i.e. used Kerberos for user authentication, then this should, in some fashion, be built into NetarchiveSuite (however, if every job is run as the same system user that runs NetarchiveSuite, no changes implementation is nessesary)

## Working on real Isilon arc/warc files

As the Narcana cluster did not have the arc/warc files on Isilon NFS mounted (why not?) I never got around to work on them. Rather I copied the NetarchiveSuite codebase into HDFS, as it contains a number of arc/warc files. These have then served as my test samples.

As I will detail below, there is no real difference (to the user) between files that are NFS mounted on every node and files that reside in HDFS. As such, I made no attempt to get the netarchive data files mounted on the narcana cluster, but this would be a reasonable next step to try.


## Merging with ArcRepositoryClient

I skipped implementing the other methods in ArcRepositoryClient, namely

```java
/**
 * Gets a single ARC record out of the ArcRepository.
 *
 * @param arcfile The name of a file containing the desired record.
 * @param index The offset of the desired record in the file
 * @return a BitarchiveRecord-object, or null if request times out or object is not found.
 * @throws IOFailure If the get operation failed.
 * @throws ArgumentNotValid if the given arcfile is null or empty string, or the given index is negative.
 */
BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid;

/**
 * Retrieves a file from an ArcRepository and places it in a local file.
 *
 * @param arcfilename Name of the arcfile to retrieve.
 * @param replica The bitarchive to retrieve the data from. On implementations with only one replica, null may be
 * used.
 * @param toFile Filename of a place where the file fetched can be put.
 * @throws IOFailure if there are problems getting a reply or the file could not be found.
 */
void getFile(String arcfilename, Replica replica, File toFile);

/**
 * Store the given file in the ArcRepository. After storing, the file is deleted.
 *
 * @param file A file to be stored. Must exist.
 * @throws IOFailure thrown if store is unsuccessful, or failed to clean up files after the store operation.
 * @throws ArgumentNotValid if file parameter is null or file is not an existing file.
 */
void store(File file) throws IOFailure, ArgumentNotValid;

/**
 * Updates the administrative data in the ArcRepository for a given file and replica.
 *
 * @param fileName The name of a file stored in the ArcRepository.
 * @param bitarchiveId The id of the replica that the administrative data for fileName is wrong for.
 * @param newval What the administrative data will be updated to.
 */
void updateAdminData(String fileName, String bitarchiveId, ReplicaStoreState newval);

/**
 * Updates the checksum kept in the ArcRepository for a given file. It is the responsibility of the ArcRepository
 * implementation to ensure that this checksum matches that of the underlying files.
 *
 * @param filename The name of a file stored in the ArcRepository.
 * @param checksum The new checksum.
 */
void updateAdminChecksum(String filename, String checksum);

/**
 * Remove a file from one part of the ArcRepository, retrieving a copy for security purposes. This is typically used
 * when repairing a file that has been corrupted.
 *
 * @param fileName The name of the file to remove.
 * @param bitarchiveId The id of the replica from which to remove the file.
 * @param checksum The checksum of the file to be removed.
 * @param credentials A string that shows that the user is allowed to perform this operation.
 * @return A local copy of the file removed.
 */
File removeAndGetFile(String fileName, String bitarchiveId, String checksum, String credentials);
```

The reason for this was that, as I understand it, Hadoop was not to be used as the distributed storage for Netarchive Data.

Now that I think about it, this implies an Isilon-bias, which I must explain further.

We have these options for disk-storage of netarchive data.

1. Store it on Isilon NAS in Aarhus. This should work directly and without problem with Hadoop. Hadoop only requires that each node in the cluster can get any file from the disk system. If the same NFS share is mounted at the same location on every node, this is trivially true.

   In this setup, we would only use HDFS as the working storage for jobs and their output, not for the real data.
 
2. Store it on a (Hadoop?) Distributed File System made up of the 43(?) Netarchive storage machines in Copenhagen. This would require us to trust HDFS as a stable bit-preservation-capable file system. I feel that this trust is warranted, but others (Tony Brian Albers <tba@kb.dk>) differs.

    Furthermore, we would need to implement a Bitrepository Pillar that could store and read files on HDFS. While HDFS is not a Posix filesystem, it comes very close, and implementing a Bitrepository pillar should be extremely simple, as the differences (append-only) match well with the bitrepository demands.

3. Store it on our custom made distributed storage in the form of <https://sbprojects.statsbiblioteket.dk/stash/projects/BITMAG/repos/kb-pillar/browse>. KB-Pillar does have the ability to run as a FrontEnd which distribute files to a number of backend pillars by some key (default is based on the first character in the file ID).

    The issue with this system is that the only (supported) way to get files is through the Bitrepository client/protocol. This way of file access is not very suitable for Hadoop jobs.
    We would thus probably implement the hadoop jobs as something that runs inside the KB-Pillar and thus would not need to use the Bitrepository client/protocol to get the files. However, then we get to the problem with "every node can read every file". This is a core requirement of Hadoop, that I do not think we can work around.
    
    The best idea, I we go with this design would probably be to implement `org.apache.hadoop.fs.Filesystem` ourselves. This have been done alot, and Hadoop ships with implementations for
    * ChRootedFileSystem
    * ChecksumFileSystem
    * DistributedFileSystem (HDFS)
    * FTPFileSystem
    * FilterFileSystem
    * HarFileSystem
    * HftpFileSystem
    * HsftpFileSystem
    * LocalFileSystem
    * RawLocalFileSystem
    * SFTPFileSystem
    * SWebHdfsFileSystem
    * ViewFileSystem
    * WebHdfsFileSystem  

    We would only need to implement the methods
    ```java
    public class BitrepositoryFileSystem extends FileSystem {
        @Override public URI getUri();
    
        @Override public FSDataInputStream open(Path f, int bufferSize) throws IOException;
    
        @Override public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize,
                short replication, long blockSize, Progressable progress) throws IOException;
    
        @Override public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException;
    
        @Override public boolean rename(Path src, Path dst) throws IOException;
    
        @Override public boolean delete(Path f, boolean recursive) throws IOException;
    
        @Override public FileStatus[] listStatus(Path f) throws FileNotFoundException, IOException;
    
        @Override public void setWorkingDirectory(Path new_dir);
    
        @Override public Path getWorkingDirectory();
    
        @Override public boolean mkdirs(Path f, FsPermission permission) throws IOException;
    
        @Override public FileStatus getFileStatus(Path f) throws IOException;
    }
    ```  
    If you squint a bit, then these correspond quite well with the methods in the Bitrepository Protocol.

