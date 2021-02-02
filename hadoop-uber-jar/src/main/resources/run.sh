#!/bin/bash

export CLASSPATH=\
/home/colin/projects/netarchivesuite/hadoop-uber-jar/target/hadoop-uber-jar-5.7-IIPCH3-SNAPSHOT-shaded.jar:\
/home/colin/projects/narchive-invoker/integrationTest/src/test/hadoopConf:\
##/home/colin/netarchivesuite/lib/harvester-core-5.7-IIPCH3-SNAPSHOT.jar:\
##/home/colin/netarchivesuite/lib/monitor-core-5.7-IIPCH3-SNAPSHOT.jar:\
##/home/colin/netarchivesuite/lib/archive-core-5.7-IIPCH3-SNAPSHOT.jar:\
$CLASSPATH;

java -Dsettings.common.hadoop.mapred.metadataExtractionJob.inputDir=/user/nat-csr/input \
 -Dsettings.common.hadoop.mapred.metadataExtractionJob.outputDir=/user/nat-csr/output \
 -Dsettings.common.hadoop.mapred.hadoopUberJar=/home/colin/projects/netarchivesuite/hadoop-uber-jar/target/hadoop-uber-jar-5.7-IIPCH3-SNAPSHOT-shaded.jar \
 MetadataIndexingApplication /user/nat-csr/9385-metadata-1.warc.gz

