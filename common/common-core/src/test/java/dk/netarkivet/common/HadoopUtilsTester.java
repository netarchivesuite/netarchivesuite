package dk.netarkivet.common;

import org.apache.hadoop.conf.Configuration;

import dk.netarkivet.common.utils.HadoopUtils;

public class HadoopUtilsTester {
    public static void main(String[] args) {
        HadoopUtils test = new HadoopUtils();
        Configuration testConf = test.getConf();
        System.out.println(testConf.get("fs.defaultFS"));
    }
}
