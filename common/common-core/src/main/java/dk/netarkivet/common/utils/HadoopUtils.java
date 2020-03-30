package dk.netarkivet.common.utils;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;

public class HadoopUtils {
    /** The class logger. */
    private static Logger log = LoggerFactory.getLogger(HadoopUtils.class);

    public Configuration getConf() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", Settings.get(CommonSettings.HADOOP_HDFS));
        //conf.set("mapreduce.framework.name", Settings.get(CommonSettings.HADOOP_MAPRED_FRAMEWORK));
        //conf.set("yarn.resourcemanager.address", Settings.get(CommonSettings.HADOOP_RESOURCEMANAGER_ADDRESS));
        return conf;
    }
}
