import java.io.OutputStream;
import java.lang.reflect.Method;

import org.archive.io.arc.ARCRecord;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.apache.commons.httpclient.URIException;

import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * This class illustrates Bug 1719.
 * The bug manifests itself with the behaviour that the class UURIFactory can be
 * loaded but that a NoClassDefFoundError is thrown during initialisation.
 */
public class ClassLoadingBugBatchJob extends ARCBatchJob {
    public void initialize(OutputStream os) {
        Class<?> urifactory = null;
        try {
            //This runs fine
            urifactory = this.getClass().getClassLoader().loadClass(
                    "org.archive.net.UURIFactory");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            //This runs fine
            Method method = urifactory.getMethod("getInstance", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        try {
            //The exception is thrown here
            UURI uurif = UURIFactory.getInstance("http://nosuch.dummy");
        } catch (URIException e) {
           throw new RuntimeException(e);
        }
    }

    public void processRecord(ARCRecord record, OutputStream os) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void finish(OutputStream os) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
