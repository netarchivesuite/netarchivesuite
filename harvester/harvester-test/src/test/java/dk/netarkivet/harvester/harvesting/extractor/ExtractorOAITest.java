package dk.netarkivet.harvester.harvesting.extractor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.extractor.Extractor;
import org.archive.crawler.extractor.Link;
import org.archive.io.ReplayCharSequence;
import org.archive.net.UURIFactory;
import org.archive.util.HttpRecorder;

@SuppressWarnings({ "serial"})
public class ExtractorOAITest extends TestCase {

    public static final String xmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" \n"
                                         + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                         + "         xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n"
                                         + "         http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\"> \n"
                                         + " <responseDate>2011-01-24T15:19:38Z</responseDate> \n"
                                         + " <request verb=\"ListRecords\"  metadataPrefix=\"oai_dc\">http://www.mtp.hum.ku.dk/library/uni/netarkiv/oai2v3/</request>\n"
                                         + " <ListRecords>\n"
                                         + "\n"
                                         + "\t<record>\n"
                                         + "    <header>\n"
                                         + "      <identifier>9788772895819,9788763500128</identifier>\n"
                                         + "      <datestamp>2010-12-06</datestamp>\n"
                                         + "\n"
                                         + "    </header>\n"
                                         + "    <metadata>\n"

                                         + "  </metadata>\n"
                                         + "  </record>\n"
                                         + "\n"
                                         + "   <resumptionToken>foobar</resumptionToken>\n"
                                         + "      \n"
                                         + "   \n"
                                         + "</ListRecords>\n"
                                         + "\n"
                                         + "</OAI-PMH>\n";
    public static final String uri = "http://www.mtp.hum.ku.dk/library/uni/netarkiv/oai2v3/?verb=ListRecords&metadataPrefix=oai_dc";

    class TestReplayCharSequence implements ReplayCharSequence {

        CharSequence seq;

        public TestReplayCharSequence(String s) {
            seq = s;
        }

        public void close() throws IOException {
        }

        public int length() {
            return seq.length();
        }

        public char charAt(int index) {
            return seq.charAt(index);
        }

        public CharSequence subSequence(int start, int end) {
            return seq.subSequence(start, end);
        }
    }

    /**
     * Create a CrawlURI corresponding to this xml and uri. Run the extract method on it.
     * Check that it now has a new link with resumptionToken=foobar in the query.
     */
    public void testExtract() throws URIException, InterruptedException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance(uri)) {
            @Override
            public HttpRecorder getHttpRecorder() {
                return new HttpRecorder(new File("/"),"") {
                    @Override
                    public ReplayCharSequence getReplayCharSequence()
                            throws IOException {
                        return new TestReplayCharSequence(xmlText);
                    }
                };
            }
        } ;
        curi.setContentType("text/xml");
        Extractor x = new ExtractorOAI("foobar"){
            @Override
            protected boolean isHttpTransactionContentToProcess(CrawlURI curi) {
                return true;
            }
        };
        x.innerProcess(curi);
        Collection<Link> links = curi.getOutLinks();
        Link link1 = links.iterator().next();
        assertTrue(link1.getDestination().toString().contains("resumptionToken=foobar"));
    }

}
