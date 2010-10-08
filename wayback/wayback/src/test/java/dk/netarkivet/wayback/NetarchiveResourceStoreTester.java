/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import junit.framework.TestCase;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.Resource;
import org.archive.wayback.exception.ResourceNotAvailableException;
import org.archive.wayback.resourcestore.resourcefile.ArcResource;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.InputStreamUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.wayback.indexer.IndexerTestCase;

/** Unit test for testNetarchiveResourceStore */
public class NetarchiveResourceStoreTester extends IndexerTestCase {

    NetarchiveResourceStore netarchiveResourceStore = null;
    CaptureSearchResult metadataResource = null;
    static CaptureSearchResult uploadResource = null;
    CaptureSearchResult resourceNotAvaliable = null;
    CaptureSearchResult httpResource = null;

    ReloadSettings rs = new ReloadSettings();
    ArcRepositoryClient arc;

    private final String metadataFile = "2-metadata-1.arc";
    private final String uploadFile = "Upload4.ARC";

    @Override
    public void setUp() {
        super.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        
        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_GET_TIMEOUT, "1000");
        assertTrue("Should get a mock connection",
            JMSConnectionFactory.getInstance() instanceof JMSConnectionMockupMQ);
        arc = (ArcRepositoryClient) ArcRepositoryClientFactory.getPreservationInstance();

        netarchiveResourceStore = new NetarchiveResourceStore();

        metadataResource = new CaptureSearchResult();
        metadataResource.setFile(metadataFile);
        metadataResource.setOffset(0L);

        uploadResource = new CaptureSearchResult();
        uploadResource.setFile(uploadFile);
        uploadResource.setOffset(2035L);

        httpResource = new CaptureSearchResult();
        httpResource.setOriginalUrl("http://www.netarkivet.dk/");
        httpResource.setOffset(0L);
        httpResource.setFile(metadataFile);


        resourceNotAvaliable = new CaptureSearchResult();
    }

    @Override
    public void tearDown() {
        arc.close();
        ArcRepository.getInstance().close();
        FileUtils.removeRecursively(dk.netarkivet.wayback.TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
        // Empty the log file.
        try {
            new FileOutputStream(dk.netarkivet.wayback.TestInfo.LOG_FILE).close();
        } catch(Exception e) {
            //ups
        }
        super.tearDown();
    }

    /**
     * Tests behavior of retriving of null CaptureSearchResult
     */
    public void testFailRetrieveResource() {
        DummyGetMessageReplyServer replyServer = new DummyGetMessageReplyServer();
        replyServer.setBitarchiveRecord(null);
        try {
            resourceNotAvaliable.getOffset();
            fail("Should cast NumberformatException");
        } catch(NumberFormatException e) {
            // Expected
        }
        try {
            netarchiveResourceStore.retrieveResource(resourceNotAvaliable);
            fail("Should have throw ResourceNotAvailableException");
        } catch (ResourceNotAvailableException e) {
            // Expected
        }

    }

    public void testRetrieveRedirect()
            throws ResourceNotAvailableException, IOException {
        String cdxLine = "netarkivet.dk/ 20090706131100 http://netarkivet.dk/ text/html 302 3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ http://netarkivet.dk/index-da.php 3311 arcfile_withredirects.arc";
        NetarchiveResourceStore store = new NetarchiveResourceStore();
        CaptureSearchResult csr = CDXLineToSearchResultAdapter.doAdapt(cdxLine);        ArcResource resource = (ArcResource) store.retrieveResource(csr);
        assertNotNull("Should have a resource", resource);
        assertTrue(resource.getRecordLength()>0);
        assertFalse(resource.getHttpHeaders().isEmpty());
        assertEquals(302, resource.getStatusCode());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resource.getArcRecord().dump(baos);
        String contents = baos.toString("UTF-8");
        assertNotNull(contents);
    }

    public void testRetrieveResource()
            throws ResourceNotAvailableException, IOException {
        String cdxLine = "ing.dk/ 20090706131100 http://ing.dk/ text/html 200 Z3UM6JX4FCO6VMVTPM6VBNJPN5D6QLO3 - 3619 arcfile_withredirects.arc";
        NetarchiveResourceStore store = new NetarchiveResourceStore();
        CaptureSearchResult csr = CDXLineToSearchResultAdapter.doAdapt(cdxLine);
        ArcResource resource = (ArcResource) store.retrieveResource(csr);
        assertNotNull("Should have a resource", resource);
        assertTrue(resource.getRecordLength()>0);
        assertFalse(resource.getHttpHeaders().isEmpty());
        assertEquals(200, resource.getStatusCode());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resource.getArcRecord().dump(baos);
        String contents = baos.toString("UTF-8");
        assertNotNull(contents);
        assertTrue(contents.contains("Motorola"));
    }

    /**
     * Test bad ARC record, but with HTTP address
     */
    /*public void testResourceWithHTTPAddresse() {
        DummyGetMessageReplyServer replyServer = new DummyGetMessageReplyServer();
        replyServer.setBitarchiveRecord(null);
        Resource resource = null;
        try {
            resource = netResource.retrieveResource(httpResource);
        } catch (ResourceNotAvailableException ex) {
            fail("Resource should be avaailable");
        }
        assertNotNull(resource);
    }*/

    /**
     * Test valid ARC record
     */
    /*public void testUploadDataRetrieveResource() {
        DummyGetMessageReplyServer replyServer = new DummyGetMessageReplyServer();
        replyServer.setBitarchiveRecord(null);
        Resource resource = null;
        try {
            resource = netarchiveResourceStore.retrieveResource(uploadResource);
        } catch (ResourceNotAvailableException e) {
            fail("Should not throw excption when retriving valid resource");
        }
        assertNotNull(resource);
        assertEquals(200, resource.getStatusCode());
        assertEquals(13442, resource.getRecordLength());


        if(resource instanceof ArcResource) {
            Map metadata = ((ArcResource)resource).getArcRecord().getHeader().getHeaderFields();
            assertEquals("Offset into file should correspond with read offset", 2038L,metadata.get(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY));
            assertEquals("Mime type not equal to ARC records", "text/html", metadata.get(ARCRecordMetaData.MIMETYPE_FIELD_KEY));
            assertEquals("URL of ARC record not equal to read URL", "http://www.netarkivet.dk/", metadata.get(ARCRecordMetaData.URL_FIELD_KEY));
            assertEquals("130.226.231.141", metadata.get(ARCRecordMetaData.IP_HEADER_FIELD_KEY));
        } else {
            fail("Should return a ArcResource at this point in time.");
        }
    }*/

    /**
     * Test ARC record with not http address, like ARC file header
     */
    public void testNonUrlRetrieveResource() {
        DummyGetMessageReplyServer replyServer = new DummyGetMessageReplyServer();
        replyServer.setBitarchiveRecord(null);
        //Resource resource = null;
        try {
            netarchiveResourceStore.retrieveResource(metadataResource);
            fail("Should have thrown ResourceNotAvailableException");
        } catch (ResourceNotAvailableException e){
            // Excpected
        }       
    }

    /**
     * Test shutdown.
     */
    public void testShutdown() {
            
    }


    /**
     * DummyGetMessageReplyServer, which acts as an intemediate JMS Server.
     * Functionality:
     *  - If ARC file exists read the approiate data into an ARC record, and create metadata information
     *  - If ARC file doesn't exists, make dummy ARC record, with no data and dummy metadata information
     */
    private static class DummyGetMessageReplyServer implements MessageListener {
        JMSConnection conn = JMSConnectionFactory.getInstance();
        private BitarchiveRecord bar;
        public boolean noReply = false;

        public DummyGetMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            if (noReply) {
                return;
            }
            try {
                GetMessage netMsg = (GetMessage) JMSConnection.unpack(msg);

                Map<String, Object> metadata = new HashMap<String, Object>();
                for (Object aREQUIRED_VERSION_1_HEADER_FIELDS : ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                    String field = (String) aREQUIRED_VERSION_1_HEADER_FIELDS;
                    metadata.put(field, "");
                }
                metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY, 0L); // Offset not stored as String but as Long
                byte[] encodedKey = encode(netMsg.getArcFile(),
                                           netMsg.getIndex());
                try {
                    String filename = netMsg.getArcFile();
                    File arcFile = new File(TestInfo.WORKING_DIR.getAbsolutePath(), filename);
                    Map headers = null;

                    uploadResource.setOriginalUrl("http://www.netarkivet.dk/");
                    uploadResource.setUrlKey("HTTP/1.1 200 OK");

                    if(arcFile.exists()) {
                        InputStream in = new FileInputStream(arcFile);
                        if(in.skip(netMsg.getIndex()) != netMsg.getIndex()) {
                            throw new IOException("InputStream read from file, which isn't long enough");
                        }
                        headers = ARCUtils.getHeadersFromARCFile(in, netMsg.getIndex());
                        in.close();
                        in = new FileInputStream(arcFile);
                        in.skip(netMsg.getIndex());
                        /*while(InputStreamUtils.readLine(in).length() == 0) {
                            // needed for testUploadDataRetrieveResource
                        } */
                        int tmp_length = new String(InputStreamUtils.readRawLine(in)).length();
                        headers.put(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY,
                                    ((Long)headers.get(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY))+ tmp_length);
                        ArchiveRecordHeader header = new ARCRecordMetaData(filename, headers);
                        ARCRecord archiveRecord = new ARCRecord(in, header);
                        bar = new BitarchiveRecord(archiveRecord, filename);
                    }

                    uploadResource.setUrlKey((String) headers.get(ARCRecordMetaData.URL_FIELD_KEY));
                    uploadResource.setMimeType((String) headers.get(ARCRecordMetaData.MIMETYPE_FIELD_KEY));
                    uploadResource.setOriginalUrl((String) headers.get(ARCRecordMetaData.IP_HEADER_FIELD_KEY));

                    netMsg.setRecord(bar);
                } catch (Exception e) {
                    e.printStackTrace();
                    metadata = new HashMap<String, Object>();
                    for (Object aREQUIRED_VERSION_1_HEADER_FIELDS : ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                        String field = (String) aREQUIRED_VERSION_1_HEADER_FIELDS;
                        metadata.put(field, "");
                    }
                    metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY, 0L);
                    try {
                        final ARCRecordMetaData meta = new ARCRecordMetaData(netMsg.getArcFile(), metadata);

                        metadata.put(ARCConstants.LENGTH_FIELD_KEY, Integer.toString(encodedKey.length));
                        setBitarchiveRecord(new BitarchiveRecord(new ARCRecord(new ByteArrayInputStream(encodedKey),meta)
                        , netMsg.getArcFile()));
                        netMsg.setRecord(bar);
                    } catch (IOException ex) {
                        throw new Error(e);
                    }


                }

                conn.reply(netMsg);
            } catch (IOFailure e) {
              // IO error
            }
        }
      public void setBitarchiveRecord(BitarchiveRecord bar) {
             this.bar = bar;
     }

     private byte[] encode(String arcFile, long index) {
        String s = arcFile + " " + index;
        return s.getBytes();
     }

     }


}
