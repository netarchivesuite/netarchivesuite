#!/bin/bash

SCRIPT_DIR=$(dirname "$(readlink -f -- ${BASH_SOURCE[0]})")

clusterUser=${1:-nat-$USER}
kerberosPrincipal=${clusterUser}@KBHPC.KB.DK

export CLASSPATH=\
$SCRIPT_DIR/hadoop-uber-jar-${project.version}-shaded.jar:\
$HOME/projects/narchive-invoker/integrationTest/src/test/hadoopConf:\
$SCRIPT_DIR/*:\
$CLASSPATH;

java \
 -Dsettings.common.hadoop.mapred.metadataExtractionJob.inputDir=/user/${clusterUser}/input \
 -Dsettings.common.hadoop.mapred.metadataExtractionJob.outputDir=/user/${clusterUser}/output \
 -Dsettings.common.hadoop.kerberos.principal=${kerberosPrincipal} \
 -Dsettings.common.hadoop.kerberos.keytab=$HOME/${clusterUser}.keytab \
 -Dsettings.common.hadoop.mapred.hadoopUberJar=$SCRIPT_DIR/hadoop-uber-jar-${project.version}-shaded.jar \
 MetadataIndexingApplication /user/${clusterUser}/9385-metadata-1.warc.gz

