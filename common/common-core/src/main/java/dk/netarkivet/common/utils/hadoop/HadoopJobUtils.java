package dk.netarkivet.common.utils.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import sun.security.krb5.KrbException;

/** Utilities for Hadoop jobs. */
public class HadoopJobUtils {
    private static final Logger log = LoggerFactory.getLogger(HadoopJobUtils.class);

    public static final String DEFAULT_FILESYSTEM = "fs.defaultFS";
    public static final String MAPREDUCE_FRAMEWORK = "mapreduce.framework.name";
    public static final String YARN_RESOURCEMANAGER_ADDRESS = "yarn.resourcemanager.address";

    /** Utility class, do not initialise. */
    private HadoopJobUtils() {
    }

    /**
     * Obtain a logged in UserGroupInformation for running hadoop jobs from the kerberos parameters
     * defined in CommonSettings.
     * @return The UserGroupInformation instance
     * @throws KrbException if the kerberos configuration is invalid
     * @throws IOException if the kerberos login fails
     */
    public static UserGroupInformation getUserGroupInformation() throws KrbException, IOException {
        String principal = Settings.get(CommonSettings.HADOOP_KERBEROS_PRINCIPAL);
        String keytab = Settings.get(CommonSettings.HADOOP_KERBEROS_KEYTAB);
        String krb5_conf = Settings.get(CommonSettings.HADOOP_KERBEROS_CONF);
        System.setProperty("java.security.krb5.conf", krb5_conf);
        sun.security.krb5.Config.refresh();
        return UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytab);
    }

    /**
     * Login to Kerberos from the settings specified in CommonSettings.
     * @throws KrbException if the kerberos configuration is invalid
     * @throws IOException if the kerberos login fails
     */
    public static void doKerberosLogin() throws KrbException, IOException {
        String principal = Settings.get(CommonSettings.HADOOP_KERBEROS_PRINCIPAL);
        String keytab = Settings.get(CommonSettings.HADOOP_KERBEROS_KEYTAB);
        String krb5_conf = Settings.get(CommonSettings.HADOOP_KERBEROS_CONF);
        System.setProperty("java.security.krb5.conf", krb5_conf);
        sun.security.krb5.Config.refresh();
        UserGroupInformation.loginUserFromKeytab(principal, keytab);
    }


    /**
     * Initialize a hadoop configuration. The basic configuration must be in a directory on the classpath. This class
     * additionally sets the path to the uber jar specified in CommonSettings#HADOOP_MAPRED_UBER_JAR
     * @return A new configuration to use for a job.
     */
    public static Configuration getConf() {
        Configuration conf = new JobConf(new YarnConfiguration(new HdfsConfiguration()));
        conf.setBoolean(MRJobConfig.JOB_AM_ACCESS_DISABLED,true);
        final String jarPath = Settings.get(CommonSettings.HADOOP_MAPRED_UBER_JAR);
        if (jarPath == null || !(new File(jarPath)).exists()) {
            log.warn("Specified jar file {} does not exist.", jarPath);
            throw new RuntimeException("Jar file " + jarPath + " does not exist.");
        }
        conf.set(MRJobConfig.JAR, jarPath);
        return conf;
    }

    /**
     * Call the set*CoresPerTask() and set*Memory BEFORE callling this, as it uses their values
     *
     * @param configuration
     * @return
     */
    public static Configuration enableUberTask(Configuration configuration, Integer appMasterMemory,
            Integer appMasterCores) {
        setAppMasterCores(configuration,
                Math.max(configuration.getInt(MRJobConfig.MAP_CPU_VCORES, MRJobConfig.DEFAULT_MAP_CPU_VCORES)
                        , configuration.getInt(MRJobConfig.REDUCE_CPU_VCORES, MRJobConfig.DEFAULT_REDUCE_CPU_VCORES))
                        + Optional.ofNullable(appMasterCores).orElse(MRJobConfig.DEFAULT_MR_AM_CPU_VCORES));
        setAppMasterMemory(configuration,
                Math.max(configuration.getInt(MRJobConfig.MAP_MEMORY_MB, MRJobConfig.DEFAULT_MAP_MEMORY_MB)
                        , configuration.getInt(MRJobConfig.REDUCE_MEMORY_MB, MRJobConfig.DEFAULT_REDUCE_MEMORY_MB))
                        + Optional.ofNullable(appMasterMemory)
                        .orElse(MRJobConfig.DEFAULT_MR_AM_VMEM_MB)); //must have enough for both the map and the reduce tasks
        configuration.setBoolean(MRJobConfig.JOB_UBERTASK_ENABLE, true);
        return configuration;
    }

    /**
     * Call the setMapCoresPerTask() and setMapMemory BEFORE callling this, as it uses their values
     *
     * @param configuration
     * @return
     */
    public static Configuration enableMapOnlyUberTask(Configuration configuration, Integer appMasterMemory,
            Integer appMasterCores) {
        setAppMasterCores(configuration,
                configuration.getInt(MRJobConfig.MAP_CPU_VCORES, MRJobConfig.DEFAULT_MAP_CPU_VCORES)
                        + Optional.ofNullable(appMasterCores).orElse(MRJobConfig.DEFAULT_MR_AM_CPU_VCORES));
        setAppMasterMemory(configuration,
                configuration.getInt(MRJobConfig.MAP_MEMORY_MB, MRJobConfig.DEFAULT_MAP_MEMORY_MB)
                        + Optional.ofNullable(appMasterMemory).orElse(MRJobConfig.DEFAULT_MR_AM_VMEM_MB));

        if (Settings.getBoolean(CommonSettings.HADOOP_MAPRED_ENABLE_UBERTASK)) {
            configuration.setBoolean(MRJobConfig.JOB_UBERTASK_ENABLE, true);
        }

        setReducerMemory(configuration, 0);
        setReduceCoresPerTask(configuration, 0);
        configuration.setInt(MRJobConfig.NUM_REDUCES, 0);

        return configuration;
    }

    public static Configuration setMapMemory(Configuration configuration, int memory) {
        configuration.setInt(MRJobConfig.MAP_MEMORY_MB, memory);
        configuration.set(MRJobConfig.MAP_JAVA_OPTS, "-Xmx" + Math.max(memory - 512, 512) + "m");
        return configuration;
    }

    public static Configuration setReducerMemory(Configuration configuration, int memory) {
        configuration.setInt(MRJobConfig.REDUCE_MEMORY_MB, memory);
        configuration.set(MRJobConfig.REDUCE_JAVA_OPTS, "-Xmx" + Math.max(memory - 512, 512) + "m");
        return configuration;
    }

    public static Configuration setAppMasterMemory(Configuration configuration, int memory) {
        configuration.setInt(MRJobConfig.MR_AM_VMEM_MB, memory);
        configuration.set(MRJobConfig.MR_AM_COMMAND_OPTS, "-Xmx" + Math.max(memory - 512, 512) + "m");
        return configuration;
    }

    public static Configuration setMapCoresPerTask(Configuration configuration, int cores) {
        configuration.setInt(MRJobConfig.MAP_CPU_VCORES, cores);
        return configuration;
    }

    public static Configuration setReduceCoresPerTask(Configuration configuration, int cores) {
        configuration.setInt(MRJobConfig.REDUCE_CPU_VCORES, cores);
        return configuration;
    }

    public static Configuration setAppMasterCores(Configuration configuration, int cores) {
        configuration.setInt(MRJobConfig.MR_AM_CPU_VCORES, cores);
        return configuration;
    }


    /**
     * Given a list of file paths prepend 'file://' to every entry and write them as newline
     * separated lines to the given input file path.
     * @param files A list of input file paths to operate on
     * @param inputFilePath The path of the file to write the lines to
     * @throws IOException If the input file path cannot be written to
     */
    public static void writeHadoopInputFileLinesToInputFile(List<java.nio.file.Path> files,
            java.nio.file.Path inputFilePath) throws IOException {
        if (files.size() == 0) {
            log.warn("No file paths to add. Input file will be empty.");
            return;
        }
        java.nio.file.Path lastElem = files.get(files.size() - 1);
        for (java.nio.file.Path file : files) {
            String inputLine = "file://" + file.toString() + "\n";
            if (file.equals(lastElem)) {
                // Not writing newline on last line to avoid a mapper being spawned on no input
                inputLine = "file://" + file.toString();
            }
            Files.write(inputFilePath, inputLine.getBytes(), StandardOpenOption.APPEND);
        }
    }

    /**
     * Collects lines from a jobs output files at a specified path.
     * Also deletes the folder once the output has been collected.
     * @param fileSystem The filesystem that the result is collected from.
     * @param outputFolder The output folder to find the job result files in.
     * @return A list of lines collected from all the output files.
     * @throws IOException If the output folder or its contents cannot be read.
     */
    public static List<String> collectOutputLines(FileSystem fileSystem, Path outputFolder) throws IOException {
        List<String> resultLines = new ArrayList<>();
        RemoteIterator<LocatedFileStatus> iterator = fileSystem.listFiles(outputFolder, false);
        while (iterator.hasNext()) {
            Path subPath = iterator.next().getPath();
            if (subPath.getName().startsWith("part-m")) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(
                        fileSystem.open(subPath))))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        resultLines.add(line);
                    }
                }
            }
        }
        // Clean up once output has been collected
        fileSystem.delete(outputFolder, true);
        return resultLines;
    }

    /**
     * TODO now here's some code that would look better with streams
     * Converts a list of CDX line strings to a list of CDXRecords
     * @param cdxLines The list to convert
     * @return A list of CDXRecords representing the old list
     */
    public static List<CDXRecord> getCDXRecordListFromCDXLines(List<String> cdxLines) {

/*      TODO when we have time to test this ...
        return cdxLines.stream()
                .map(line -> line.split("\\s+"))
                .map(split -> new CDXRecord(split))
                .collect(Collectors.toList());*/

        List<CDXRecord> recordsForJob = new ArrayList<>();
        for (String line : cdxLines) {
            String[] parts = line.split("\\s+");
            CDXRecord record = new CDXRecord(parts);
            recordsForJob.add(record);
        }
        return recordsForJob;
    }

    public static void configureCaching(Configuration configuration) {
        configuration.setBoolean(CommonSettings.HADOOP_ENABLE_HDFS_CACHE, Settings.getBoolean(CommonSettings.HADOOP_ENABLE_HDFS_CACHE));
        configuration.set(CommonSettings.HADOOP_HDFS_CACHE_DIR, Settings.get(CommonSettings.HADOOP_HDFS_CACHE_DIR));
        configuration.setInt(CommonSettings.HADOOP_CACHE_DAYS, Settings.getInt(CommonSettings.HADOOP_CACHE_DAYS));
    }

}
