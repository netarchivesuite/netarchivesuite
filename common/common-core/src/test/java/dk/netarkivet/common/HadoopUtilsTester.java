package dk.netarkivet.common;

import org.apache.hadoop.conf.Configuration;

import dk.netarkivet.common.utils.HadoopUtils;
import dk.netarkivet.common.utils.Settings;

public class HadoopUtilsTester {
    public static void main(String[] args) {
        Configuration testConf = HadoopUtils.getConfFromSettings();
        System.out.println(testConf.get("fs.defaultFS"));
        System.out.println(Settings.getBoolean(CommonSettings.USING_HADOOP));
    }
}
