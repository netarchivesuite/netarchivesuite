#!/usr/bin/env bash

set -e
set -x

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

cd $SCRIPT_DIR

mvn -DskipTests -Phortonworks -Psbprojects-nexus install -pl mass-processing -am

scp ~/Projects/netarchivesuite/mass-processing/target/mass-processing-5.6-SNAPSHOT-jar-with-dependencies.jar dkm_eld@narcana-webdanica01:.

ssh dkm_eld@narcana-webdanica01 \
    yarn jar mass-processing-5.6-SNAPSHOT-jar-with-dependencies.jar dk.netarkivet.common.utils.archive.ArchiveChecksumJob hdfs://HDFS/user/dkm_eld/netarchivesuite/

#Previously, I copied the netarchive codebase to HDFS so we would have some files to work on