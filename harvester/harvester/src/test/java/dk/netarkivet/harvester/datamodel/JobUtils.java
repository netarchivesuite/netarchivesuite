package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.testutils.ReflectUtils;

public class JobUtils {
    
    /** Get a simple job with low priority
     * @return a simple job with low priority
     */
    public static Job getJobLowPriority(File ORDER_FILE) {
        try {
            HarvestChannel lowChan = new HarvestChannel("SNAPSHOT", true, true, "");
            Constructor<Job> c = ReflectUtils.getPrivateConstructor(
                    Job.class, Long.class, Map.class, String.class, Boolean.TYPE, 
                    Long.TYPE, Long.TYPE, Long.TYPE, JobStatus.class, String.class, 
                    Document.class, String.class, Integer.TYPE, Long.class);
            return c.newInstance(42L, Collections.<String, String>emptyMap(),
                                 lowChan.getName(), lowChan.isSnapshot(), -1L, -1L, 0L,
                                 JobStatus.NEW, "default_template",
                                 XmlUtils.getXmlDoc(ORDER_FILE),
                                 "www.netarkivet.dk", 1, (Long) null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /** Get a simple job with high priority.
     *  @return a simple job with high priority
     */
    public static Job getHighPriorityJob(File ONE_LEVEL_ORDER_FILE, JobStatus jobstate, String templateName) {
        Document d = XmlUtils.getXmlDoc(ONE_LEVEL_ORDER_FILE);
        try {
            HarvestChannel highChan = new HarvestChannel("FOCUSED", false, true, "");
            Constructor<Job> c = ReflectUtils.getPrivateConstructor(
                    Job.class, Long.class, Map.class, String.class, Boolean.TYPE, 
                    Long.TYPE, Long.TYPE, Long.TYPE, JobStatus.class, String.class, 
                    Document.class, String.class, Integer.TYPE, Long.class);
            String seedList = "www.netarkivet.dk";
            return c.newInstance(42L, Collections.<String, String>emptyMap(),
                                 highChan.getName(), highChan.isSnapshot(), -1L, -1L, 0L,
                                 jobstate, templateName, d, seedList, 1, (Long) null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Job getSnapshotJobWithSbforgeAsSeed() throws NoSuchMethodException,
    IllegalAccessException,
    InvocationTargetException,
    InstantiationException {
        Constructor<Job> c = ReflectUtils.getPrivateConstructor(
                Job.class, Long.class, Map.class, String.class, Boolean.TYPE, 
                Long.TYPE,Long.TYPE, Long.TYPE, JobStatus.class, String.class, Document.class,
                String.class, Integer.TYPE, Long.class);
        HarvestChannel lowChan = new HarvestChannel("SNAPSHOT", true, true, "");
        return c.newInstance(42L, Collections.<String, String>emptyMap(),
                lowChan.getName(), lowChan.isSnapshot(), -1L, -1L, 0L,
                JobStatus.STARTED, "default_template",
                DocumentFactory.getInstance().createDocument(),
                "http://sbforge.org", 1, null);
    }
    
    
    
    

    
    
    
    
}
