/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting.extractor;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Collection;

import org.apache.commons.httpclient.URIException;
import org.archive.io.ReplayCharSequence;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.Extractor;
import org.archive.net.UURIFactory;
import org.archive.util.Recorder;
//import org.archive.util.HttpRecorder;
import org.junit.Test;

@SuppressWarnings({"serial"})
public class ExtractorOAITest {

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
            + "      \n" + "   \n" + "</ListRecords>\n" + "\n" + "</OAI-PMH>\n";
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
        /** FIXME **/
		public Charset getCharset() {
			// TODO Auto-generated method stub
			return null;
		}
		/** FIXME **/
		public CharacterCodingException getCodingException() {
			// TODO Auto-generated method stub
			return null;
		}
		/** FIXME **/
		public long getDecodeExceptionCount() {
			// TODO Auto-generated method stub
			return 0;
		}
		/** FIXME **/
		public boolean isOpen() {
			// TODO Auto-generated method stub
			return false;
		}
    }

    /**
     * Create a CrawlURI corresponding to this xml and uri. Run the extract method on it. Check that it now has a new
     * link with resumptionToken=foobar in the query.
     */
    //TODO fix the unittest
    public void testExtract() throws URIException, InterruptedException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance(uri)) {
            @Override
            public Recorder getRecorder() {
                return new Recorder(new File("/"), "") {
                    @Override
                    public ReplayCharSequence getReplayCharSequence() throws IOException {
                        return new TestReplayCharSequence(xmlText);
                    }
                };
            }
        };
        curi.setContentType("text/xml");
        Extractor x = new ExtractorOAI() {
        	//FIXME the isHttpTransactionContentToProcess method no longer exists
        	// Probably replace with ???? Look in Migrated ExtractorOAI
        	/*
            @Override
            protected boolean isHttpTransactionContentToProcess(CrawlURI curi) {
                return true;
            }*/
        };
        // The innerProcess no longer exists in the Extractor API
        //x.innerProcess(curi);
        // TODO check if this works.
        x.process(curi);
        Collection<CrawlURI> links = curi.getOutLinks();
        CrawlURI link1 = links.iterator().next();
        // .destination not long exists.
        assertTrue(link1.getURI().toString().contains("resumptionToken=foobar"));
    }
}
