package dk.netarkivet.wayback;

import java.io.InputStream;
import java.io.OutputStream;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 9:52:19 AM To
 * change this template use File | Settings | File Templates.
 */
public class DeduplicateToCDXAdapter implements DeduplicateToCDXAdapterInterface {

    

    public String adaptLine(String line) {
       throw new NotImplementedException("not yet implemented");
    }

    public void adaptStream(InputStream is, OutputStream os) {
       throw new NotImplementedException("not yet implemented");
    }
}
