import org.apache.commons.httpclient.URIException;
import org.archive.net.UURIFactory;

import dk.netarkivet.common.utils.DomainUtils;


public class TestSeedUri {
    public static void main(String[] args) throws URIException {
        String sourceCandidate = "http://www.netarkivet.dk/";

        String hostname = UURIFactory.getInstance(sourceCandidate).getHost();
        
        System.out.println(DomainUtils.domainNameFromHostname(hostname));
    }
}
