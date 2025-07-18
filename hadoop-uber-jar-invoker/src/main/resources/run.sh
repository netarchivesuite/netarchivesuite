#!/bin/bash

SCRIPT_DIR=$(dirname "$(readlink -f -- ${BASH_SOURCE[0]})")

if [ ! -d "$SCRIPT_DIR/libs" ]; then
  echo "This script must be run from the version in the target directory after compilation."
  exit 1
fi

clusterUser=${1:-nat-$USER}
kerberosPrincipal=${clusterUser}@KBHPC.KB.DK
keytab=${3:-$HOME/${clusterUser}.keytab}
hadoopConf=${4:-$HOME/projects/narchive-invoker/integrationTest/src/test/hadoopConf}
inputFile=${2:-$SCRIPT_DIR/../../..src/main/resources/input.txt}


export CLASSPATH=\
$SCRIPT_DIR/libs/hadoop-uber-jar-${project.version}-shaded.jar:\
$hadoopConf:\
$SCRIPT_DIR/libs/*:\
$CLASSPATH;

java \
 --add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED \
 -Dsettings.common.hadoop.mapred.metadataExtractionJob.inputDir=/user/${clusterUser}/input \
 -Dsettings.common.hadoop.mapred.metadataExtractionJob.outputDir=/user/${clusterUser}/output \
 -Dsettings.common.hadoop.kerberos.principal=${kerberosPrincipal} \
 -Dsettings.common.hadoop.kerberos.keytab=$keytab \
 -Dsettings.common.hadoop.mapred.hadoopUberJar=$SCRIPT_DIR/libs/hadoop-uber-jar-${project.version}-shaded.jar \
 -Dsettings.common.hadoop.kerberos.krb5-conf=/etc/krb5.conf \
 MetadataIndexingApplication "$inputFile"

