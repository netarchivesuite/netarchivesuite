# HDFS File Security

Her er det relevante dokument om hvordan HDFS virker, og hvad sikkerhed der er mod data korruption.

<https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html>


Jeg har uddraget et par relevante afsnit her


## Data Integrity
It is possible that a block of data fetched from a DataNode arrives corrupted. This corruption can occur because of faults in a storage device, network faults, or buggy software. The HDFS client software implements checksum checking on the contents of HDFS files. When a client creates an HDFS file, it computes a checksum of each block of the file and stores these checksums in a separate hidden file in the same HDFS namespace. When a client retrieves file contents it verifies that the data it received from each DataNode matches the checksum stored in the associated checksum file. If not, then the client can opt to retrieve that block from another DataNode that has a replica of that block.


## Data Blocks
HDFS is designed to support very large files. Applications that are compatible with HDFS are those that deal with large data sets. These applications write their data only once but they read it one or more times and require these reads to be satisfied at streaming speeds. HDFS supports write-once-read-many semantics on files. A typical block size used by HDFS is 128 MB. Thus, an HDFS file is chopped up into 128 MB chunks, and if possible, each chunk will reside on a different DataNode.


## Replication Pipelining
When a client is writing data to an HDFS file with a replication factor of three, the NameNode retrieves a list of DataNodes using a replication target choosing algorithm. This list contains the DataNodes that will host a replica of that block. The client then writes to the first DataNode. The first DataNode starts receiving the data in portions, writes each portion to its local repository and transfers that portion to the second DataNode in the list. The second DataNode, in turn starts receiving each portion of the data block, writes that portion to its repository and then flushes that portion to the third DataNode. Finally, the third DataNode writes the data to its local repository. Thus, a DataNode can be receiving data from the previous one in the pipeline and at the same time forwarding data to the next one in the pipeline. Thus, the data is pipelined from one DataNode to the next.


## Andet

Den her er også interessant
<https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/ArchivalStorage.html>


Her er et par artikler og præsentationer der måske er interessante
* <https://indico.cern.ch/event/214784/contributions/1512447/attachments/340854/475673/storage_donvito_chep_2013.pdf>
* <https://dl.acm.org/citation.cfm?id=3229269>


## HDFS 3

<https://hortonworks.com/blog/hadoop-3-adds-value-hadoop-2/>

### Erasure Coding
Hadoop 2 has a lot more storage overhead than Hadoop 3. For example, in Hadoop 2, if there are 6 blocks and 3x replication of each block, the result will be 18 blocks of space.

With erasure coding in Hadoop 3, if there are 6 blocks, it will occupy a 9 block space – 6 blocks and 3 for parity – resulting in less storage overhead.  The end result -instead of the 3x hit on storage, the erasure coding storage method will incur an overhead of 1.5x, while maintaining the same level of data recoverability. It halves the storage cost of HDFS while also retaining data durability.  Storage overhead can be reduced from 200% to 50%. In addition, you benefit from the tremendous cost savings.

### Scalability

Hadoop 2 and Hadoop 1 only use a single NameNode to manage all Namespaces. Hadoop 3 has multiple Namenodes for multiple namespaces for NameNode Federation which improves scalability.

In Hadoop 2, there is only one standby NameNode.  Hadoop 3 supports multiple standby NameNodes. If one standby node goes down over the weekend, you have the benefit of other standby NameNodes so the cluster can continue to operate.  This feature gives you a longer servicing window.

Hadoop 2 cannot accommodate intra-node disk balancing. Hadoop 3 has intra-node disk balancing. If you are repurposing or adding new storage to an existing server with older capacity drives, this leads to unevenly disks space in each server. With intra-node disk balancing, the space in each disk is evenly distributed.

