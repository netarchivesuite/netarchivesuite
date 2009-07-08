package dk.netarkivet.wayback;

/* $ ID: NetarchiveResourceStoreTester.java Jun 15, 2009 10:30:10 AM hbk $
* $ Revision: $
* $ Date: Jun 15, 2009 10:30:10 AM $ 
* $ @auther hbk $
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.Resource;
import org.archive.wayback.exception.ResourceNotAvailableException;
import org.archive.wayback.resourcestore.resourcefile.ArcResource;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.arcrepository.distribute.ArcRepositoryServer;
import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.archive.bitarchive.Bitarchive;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.InputStreamUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** Unit test for testNetarchiveResourceStore */
public class NetarchiveResourceStoreTester extends TestCase {

    NetarchiveResourceStore netResource = null;
    CaptureSearchResult metadataResource = null;
    static CaptureSearchResult uploadResouce = null;
    CaptureSearchResult resourceNotAvaliable = null;
    CaptureSearchResult httpResource = null;

    ReloadSettings rs = new ReloadSettings();
    Bitarchive archive;
    BitarchiveMonitorServer bitarchiveMonitor;
    LocalArcRepositoryClient arcRepo;
    ArcRepositoryServer arcServer;
    ArcRepositoryClientFactory arcRCF;
    JMSArcRepositoryClient arc;

    private final String metadataFile = "2-metadata-1.arc";
    private final String uploadFile = "Upload4.ARC";

    @Override
    public void setUp() {
        rs.setUp();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        
        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_GET_TIMEOUT, "1000");

        arc = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getPreservationInstance();
        
        netResource = new NetarchiveResourceStore();

        metadataResource = new CaptureSearchResult();
        metadataResource.setFile(metadataFile);
        metadataResource.setOffset(0L);

        uploadResouce = new CaptureSearchResult();
        uploadResouce.setFile(uploadFile);
        uploadResouce.setOffset(1485L);

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
        rs.tearDown();
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
            netResource.retrieveResource(resourceNotAvaliable);
            fail("Should have throw ResourceNotAvailableException");
        } catch (ResourceNotAvailableException e) {
            // Expected
        }

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
    public void testUploadDataRetrieveResource() {
        DummyGetMessageReplyServer replyServer = new DummyGetMessageReplyServer();
        replyServer.setBitarchiveRecord(null);
        Resource resource = null;
        try {
            resource = netResource.retrieveResource(uploadResouce);
        } catch (ResourceNotAvailableException e) {
            fail("Should not throw excption when retriving valid resource");
        }
        assertNotNull(resource);
        assertEquals(404, resource.getStatusCode());
        assertEquals(472, resource.getRecordLength());


        if(resource instanceof ArcResource) {
            Map metadata = ((ArcResource)resource).getArcRecord().getHeader().getHeaderFields();
            assertEquals("Offset into file should correspond with read offset", 1485L,metadata.get(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY));
            assertEquals("Mime type not equal to ARC records", "text/html", metadata.get(ARCRecordMetaData.MIMETYPE_FIELD_KEY));
            assertEquals("URL of ARC record not equal to read URL", "http://www.netarkivet.dk/robots.txt", metadata.get(ARCRecordMetaData.URL_FIELD_KEY));
            assertEquals("130.226.231.141", metadata.get(ARCRecordMetaData.IP_HEADER_FIELD_KEY));
        } else {
            fail("Should return a ArcResource at this point in time.");
        }
    }

    /**
     * Test ARC record with not http address, like ARC file header
     */
    public void testNonUrlRetrieveResource() {
        DummyGetMessageReplyServer replyServer = new DummyGetMessageReplyServer();
        replyServer.setBitarchiveRecord(null);
        //Resource resource = null;
        try {
            netResource.retrieveResource(metadataResource);
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

                    uploadResouce.setOriginalUrl("http://www.netarkivet.dk/");
                    uploadResouce.setUrlKey("HTTP/1.1 200 OK\r\n");

                    if(arcFile.exists()) {
                        InputStream in = new FileInputStream(arcFile);
                        if(in.skip(netMsg.getIndex()) != netMsg.getIndex()) {
                            throw new IOException("InputStream read from file, which isn't long enough");
                        }
                        headers = ARCUtils.getHeadersFromARCFile(in, netMsg.getIndex());
                        in.close();
                        in = new FileInputStream(arcFile);
                        if(in.skip(netMsg.getIndex()) != netMsg.getIndex()) {
                            throw new IOException("InputStream read from file, which isn't long enough");
                        }
                        while(InputStreamUtils.readLine(in).length() == 0) {
                            // needed for testUploadDataRetrieveResource
                        }
                        ArchiveRecordHeader header = new ARCRecordMetaData(filename, headers);
                        ARCRecord archiveRecord = new ARCRecord(in, header);
                        bar = new BitarchiveRecord(archiveRecord);
                    }

                    uploadResouce.setUrlKey((String) headers.get(ARCRecordMetaData.URL_FIELD_KEY));
                    uploadResouce.setMimeType((String) headers.get(ARCRecordMetaData.MIMETYPE_FIELD_KEY));
                    uploadResouce.setOriginalUrl((String) headers.get(ARCRecordMetaData.IP_HEADER_FIELD_KEY));

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
                        setBitarchiveRecord(new BitarchiveRecord(new ARCRecord(new ByteArrayInputStream(encodedKey),meta)));
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
