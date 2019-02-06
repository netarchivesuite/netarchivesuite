Narcana "test that the system works" proceduce
==============================================

Run 
```{bash}
ssh dkm_eld@narcana-webdanica01.statsbiblioteket.dk yarn jar /usr/hdp/current/hadoop-mapreduce-client/hadoop-mapreduce-examples.jar pi 32 100000
```
TODO: change this to use the `narcana-suite01.statsbiblioteket.dk` machine when it comes online

This logs into a machine and runs a "calc pi" example to test that the cluster works

The output should be something like this

```
Number of Maps  = 32
Samples per Map = 100000
Wrote input for Map #0
Wrote input for Map #1
Wrote input for Map #2
Wrote input for Map #3
Wrote input for Map #4
Wrote input for Map #5
Wrote input for Map #6
Wrote input for Map #7
Wrote input for Map #8
Wrote input for Map #9
Wrote input for Map #10
Wrote input for Map #11
Wrote input for Map #12
Wrote input for Map #13
Wrote input for Map #14
Wrote input for Map #15
Wrote input for Map #16
Wrote input for Map #17
Wrote input for Map #18
Wrote input for Map #19
Wrote input for Map #20
Wrote input for Map #21
Wrote input for Map #22
Wrote input for Map #23
Wrote input for Map #24
Wrote input for Map #25
Wrote input for Map #26
Wrote input for Map #27
Wrote input for Map #28
Wrote input for Map #29
Wrote input for Map #30
Wrote input for Map #31
Starting Job
19/02/06 10:38:54 INFO client.AHSProxy: Connecting to Application History server at narcana-ambari01.statsbiblioteket.dk/172.16.221.201:10200
19/02/06 10:38:54 INFO client.RequestHedgingRMFailoverProxyProvider: Looking for the active RM in [rm1, rm2]...
19/02/06 10:38:54 INFO client.RequestHedgingRMFailoverProxyProvider: Found active RM [rm1]
19/02/06 10:38:54 INFO input.FileInputFormat: Total input paths to process : 32
19/02/06 10:38:54 INFO mapreduce.JobSubmitter: number of splits:32
19/02/06 10:38:54 INFO mapreduce.JobSubmitter: Submitting tokens for job: job_1538986315878_0073
19/02/06 10:38:55 INFO impl.YarnClientImpl: Submitted application application_1538986315878_0073
19/02/06 10:38:55 INFO mapreduce.Job: The url to track the job: http://narcana-yarn01.statsbiblioteket.dk:8088/proxy/application_1538986315878_0073/
19/02/06 10:38:55 INFO mapreduce.Job: Running job: job_1538986315878_0073
19/02/06 10:39:00 INFO mapreduce.Job: Job job_1538986315878_0073 running in uber mode : false
19/02/06 10:39:00 INFO mapreduce.Job:  map 0% reduce 0%
19/02/06 10:39:08 INFO mapreduce.Job:  map 41% reduce 0%
19/02/06 10:39:09 INFO mapreduce.Job:  map 91% reduce 0%
19/02/06 10:39:11 INFO mapreduce.Job:  map 100% reduce 0%
19/02/06 10:39:12 INFO mapreduce.Job:  map 100% reduce 100%
19/02/06 10:39:12 INFO mapreduce.Job: Job job_1538986315878_0073 completed successfully
19/02/06 10:39:12 INFO mapreduce.Job: Counters: 50
	File System Counters
		FILE: Number of bytes read=710
		FILE: Number of bytes written=4931245
		FILE: Number of read operations=0
		FILE: Number of large read operations=0
		FILE: Number of write operations=0
		HDFS: Number of bytes read=8214
		HDFS: Number of bytes written=215
		HDFS: Number of read operations=131
		HDFS: Number of large read operations=0
		HDFS: Number of write operations=3
	Job Counters 
		Launched map tasks=32
		Launched reduce tasks=1
		Data-local map tasks=28
		Rack-local map tasks=4
		Total time spent by all maps in occupied slots (ms)=1037430
		Total time spent by all reduces in occupied slots (ms)=17340
		Total time spent by all map tasks (ms)=207486
		Total time spent by all reduce tasks (ms)=1734
		Total vcore-milliseconds taken by all map tasks=207486
		Total vcore-milliseconds taken by all reduce tasks=1734
		Total megabyte-milliseconds taken by all map tasks=1062328320
		Total megabyte-milliseconds taken by all reduce tasks=17756160
	Map-Reduce Framework
		Map input records=32
		Map output records=64
		Map output bytes=576
		Map output materialized bytes=896
		Input split bytes=4438
		Combine input records=0
		Combine output records=0
		Reduce input groups=2
		Reduce shuffle bytes=896
		Reduce input records=64
		Reduce output records=0
		Spilled Records=128
		Shuffled Maps =32
		Failed Shuffles=0
		Merged Map outputs=32
		GC time elapsed (ms)=2645
		CPU time spent (ms)=38920
		Physical memory (bytes) snapshot=76612993024
		Virtual memory (bytes) snapshot=215665999872
		Total committed heap usage (bytes)=77488717824
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	File Input Format Counters 
		Bytes Read=3776
	File Output Format Counters 
		Bytes Written=97
Job Finished in 18.316 seconds
Estimated value of Pi is 3.14158625000000000000
```
