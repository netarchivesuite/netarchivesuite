/*
 * #%L
 * Netarchivesuite - wayback - test
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
package dk.netarkivet.wayback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.archive.io.ArchiveRecordHeader;
import org.archive.format.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.exception.ResourceNotAvailableException;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;
import org.archive.wayback.resourcestore.resourcefile.ArcResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.utils.InputStreamUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** Unit test for testNetarchiveResourceStore */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class NetarchiveResourceStoreTester {
    @Rule public TestName test = new TestName();
    private File WORKING_DIR;

    NetarchiveResourceStore netarchiveResourceStore = null;
    CaptureSearchResult metadataResource = null;
    static CaptureSearchResult uploadResource = null;
    CaptureSearchResult resourceNotAvaliable = null;
    CaptureSearchResult httpResource = null;

    ReloadSettings rs = new ReloadSettings();
    ArcRepositoryClient arc;

    private final String metadataFile = "2-metadata-1.arc";
    private final String uploadFile = "Upload4.ARC";

    @Before
    public void setUp() {
        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, "dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient");
        Settings.set("settings.common.arcrepositoryClient.fileDir", "test/testdata/archive");

        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_GET_TIMEOUT, "1000");
        arc = (ArcRepositoryClient) ArcRepositoryClientFactory.getPreservationInstance();

        netarchiveResourceStore = new NetarchiveResourceStore();

        metadataResource = new CaptureSearchResult();
        metadataResource.setFile(metadataFile);
        metadataResource.setOffset(0L);

        uploadResource = new CaptureSearchResult();
        uploadResource.setFile(uploadFile);
        uploadResource.setOffset(2041L);

        httpResource = new CaptureSearchResult();
        httpResource.setOriginalUrl("http://www.netarkivet.dk/");
        httpResource.setOffset(0L);
        httpResource.setFile(metadataFile);

        resourceNotAvaliable = new CaptureSearchResult();
    }

    @After
    public void tearDown() {
        arc.close();
    }

    @Test(expected = NumberFormatException.class)
    public void testRetrieveResourceNullRecord() {
        resourceNotAvaliable.getOffset();
    }

    @Test(expected = ResourceNotAvailableException.class)
    public void testResourceNotAvailable() throws ResourceNotAvailableException {
        netarchiveResourceStore.retrieveResource(resourceNotAvaliable);
    }

    @Test
    public void testRetrieveRedirect() throws ResourceNotAvailableException, IOException {
        String cdxLine = "netarkivet.dk/ 20090706131100 http://netarkivet.dk/ text/html 302 3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ http://netarkivet.dk/index-da.php 3311 arcfile_withredirects.arc";
        NetarchiveResourceStore store = new NetarchiveResourceStore();
        CDXLineToSearchResultAdapter cdxAdapter = new CDXLineToSearchResultAdapter();
        CaptureSearchResult csr = cdxAdapter.adapt(cdxLine);
        ArcResource resource = (ArcResource) store.retrieveResource(csr);
        assertNotNull("Should have a resource", resource);
        assertTrue(resource.getRecordLength() > 0);
        assertEquals(302, resource.getStatusCode());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resource.getArcRecord().dump(baos);
        String contents = baos.toString("UTF-8");
        assertNotNull(contents);
    }

    @Test
    public void testRetrieveResource() throws ResourceNotAvailableException, IOException {
        String cdxLine = "ing.dk/ 20090706131100 http://ing.dk/ text/html 200 Z3UM6JX4FCO6VMVTPM6VBNJPN5D6QLO3 - 3619 arcfile_withredirects.arc";
        NetarchiveResourceStore store = new NetarchiveResourceStore();
        CDXLineToSearchResultAdapter cdxAdapter = new CDXLineToSearchResultAdapter();
        CaptureSearchResult csr = cdxAdapter.adapt(cdxLine);
        ArcResource resource = (ArcResource) store.retrieveResource(csr);
        assertNotNull("Should have a resource", resource);
        assertTrue(resource.getRecordLength() > 0);
        assertEquals(200, resource.getStatusCode());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resource.getArcRecord().dump(baos);
        String contents = baos.toString("UTF-8");
        assertNotNull(contents);
        assertTrue(contents.contains("Motorola"));
    }



    /**
     * Test ARC record with non http address, like ARC file header.
     */
    @Test(expected = ResourceNotAvailableException.class)
    public void testNonUrlRetrieveResource() throws ResourceNotAvailableException {
        netarchiveResourceStore.retrieveResource(metadataResource);
        fail("Should have thrown ResourceNotAvailableException");
    }

    /**
     * Test shutdown.
     */
    @Test
    public void testShutdown() {

    }

    /**
     * DummyGetMessageReplyServer, which acts as an intermediate JMS Server. Functionality: - If ARC file exists read
     * the appropriate data into an ARC record, and create metadata information - If ARC file doesn't exists, make dummy
     * ARC record, with no data and dummy metadata information
     */
    private class DummyGetMessageReplyServer implements MessageListener {
        JMSConnection conn = JMSConnectionFactory.getInstance();
        private BitarchiveRecord bar;
        public boolean noReply = false;

        public DummyGetMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        @SuppressWarnings("resource")
        public void onMessage(Message msg) {
            if (noReply) {
                return;
            }
            GetMessage netMsg = (GetMessage) JMSConnection.unpack(msg);

            Map<String, Object> metadata = new HashMap<String, Object>();
            for (Object aREQUIRED_VERSION_1_HEADER_FIELDS : ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                String field = (String) aREQUIRED_VERSION_1_HEADER_FIELDS;
                metadata.put(field, "");
            }
            metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY, 0L); // Offset
            // not stored as String but as Long
            byte[] encodedKey = encode(netMsg.getArcFile(), netMsg.getIndex());
            try {
                String filename = netMsg.getArcFile();
                File arcFile = new File(WORKING_DIR.getAbsolutePath(), filename);
                Map headers = null;

                uploadResource.setOriginalUrl("http://www.netarkivet.dk/");
                uploadResource.setUrlKey("HTTP/1.1 200 OK");

                if (arcFile.exists()) {
                    InputStream in = new FileInputStream(arcFile);
                    if (in.skip(netMsg.getIndex()) != netMsg.getIndex()) {
                        throw new IOException("InputStream read from file, which isn't long enough");
                    }
                    headers = ARCUtils.getHeadersFromARCFile(in, netMsg.getIndex());
                    in.close();
                    in = new FileInputStream(arcFile);
                    in.skip(netMsg.getIndex());
                        /*
                         * while(InputStreamUtils.readLine(in).length() == 0) { // needed for
                         * testUploadDataRetrieveResource }
                         */
                    int tmp_length = new String(InputStreamUtils.readRawLine(in)).length();
                    headers.put(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY,
                            ((Long) headers.get(ARCRecordMetaData.ABSOLUTE_OFFSET_KEY)) + tmp_length);
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
                    setBitarchiveRecord(new BitarchiveRecord(new ARCRecord(new ByteArrayInputStream(encodedKey),
                            meta), netMsg.getArcFile()));
                    netMsg.setRecord(bar);
                } catch (IOException ex) {
                    throw new Error(e);
                }

            }
            conn.reply(netMsg);
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
