import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import gnu.inet.encoding.IDNA;
import gnu.inet.encoding.IDNAException;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.httpclient.URIException;
import org.archive.io.arc.ARCRecord;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;

import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteUURIFactory;

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
            os.write(("\n"+UURIFactory.IGNORED_SCHEME+"\n").getBytes());            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        UURI uuri = null;
        try {
            uuri = new UURI("http://foo.bar", false){

            };
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
        try {
            os.write(("Created UURI:'" + uuri.toString()+"'\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s1 = null;
        try {
            s1 = IDNA.toASCII("astringø");
        } catch (IDNAException e) {
            throw new RuntimeException(e);
        }
        try {
            os.write(("IDNA'ed a string:'" + s1 + "'\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MutableString ms = new MutableString("hello world");
        try {
            os.write(("created a mutable string:'"+ms.toString()+"'\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            uuri = NetarchiveSuiteUURIFactory.getInstance("http://foo.bar");
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
        try {
            os.write(("Created a uuri from cut'n'paste factory: '" + uuri.toString() + "'\n").getBytes());
        } catch (IOException e) {
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
