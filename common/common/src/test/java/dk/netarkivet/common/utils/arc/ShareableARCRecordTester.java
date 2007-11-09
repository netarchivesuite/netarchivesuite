/*$Id$
* $Revision$
* $Date$
* $Author$
*
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
package dk.netarkivet.common.utils.arc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.common.utils.FileUtils;

public class ShareableARCRecordTester extends TestCase
{
  //Reference to test file:
  private static final String ARC_DIR = "tests/dk/netarkivet/common/utils/arc/data/input/";
  private static final String ARC_FILE_NAME = "fyensdk.arc";
  private static final String RECORD_CONTENT_FILE_NAME = "last.obj";
  //Describe the used test record:
  private static final long OFFSET = 643869;
  private static final String URL = "http://www.fyens.dk/picturecache/imageseries/getpicture.php?Width=100&pictureid=400";
  private static final String IP = "194.255.126.118";
  private static final String DATE_TIME = "20040511211327";
  private static final String MIME_TYPE = "image/jpeg";
  private static final long LENGTH = 3837;
  private static byte[] correctContent;
  //Our main instance of ShareableARCRecord:
  private ShareableARCRecord sar;
  //Variables used for constructing sar:
  private File testFile;
  private ARCReader arcReader;
  private ARCRecord testRec;

  public ShareableARCRecordTester() throws IOException{
  }
  protected void setUp() throws IOException
  {
      correctContent = FileUtils.readBinaryFile(new File(ARC_DIR, RECORD_CONTENT_FILE_NAME));
    testFile = new File(ARC_DIR, ARC_FILE_NAME);
    arcReader = ARCReaderFactory.get(testFile);
    testRec = (ARCRecord) (arcReader.get(OFFSET));
    //TODO: Construction of sar should be put in each method? What if it fails?
    sar = new ShareableARCRecord(testRec,testFile);
  }
  protected void tearDown() throws IOException {
    testRec.close();
    arcReader.close();
  }
  /**
   * Verify that the constructor fails if it is given a null ARCRecord.
   * We allow a null File for flexibility.
   */
  public void testConstructorWithNull()
  {
    ShareableARCRecord otherSar;
    try{
        otherSar = new ShareableARCRecord(testRec,null);
        fail("Should not get a result with null parameter: " + sar);
    } catch (Exception e)
    {
    }
    try{
      otherSar = new ShareableARCRecord(null,testFile);
      fail("Should not get a result with null parameter: " + sar.toString());
    } catch (Exception e)
    {
      //This is correct.
    }
    try{
      otherSar = new ShareableARCRecord(null,null);
      fail("Should not get a result with null parameter: " + sar.toString());
    } catch (Exception e)
    {
      //This is correct.
    }
  }
  /**
   * Verify that regular construction of a ShareableARCRecord suceeds.
   */
  public void testConstructor()
  {
    try{
      sar = new ShareableARCRecord(testRec,testFile);
    } catch (Exception e)
    {
      fail(e.toString());
    }
    //This is correct.
  }
  /**
   * Verify that getFile() returns the same File that was given in the constructor.
   */
  public void testGetFile()
  {
    File retFile = sar.getFile();
    assertEquals(testFile,retFile);
  }
  /**
   * Verify that the returned metadata matches the metadata in the provided record.
   */
  public void testGetMetadata()
  {
    ARCRecordMetaData met = sar.getMetaData();
    assertEquals(met.getUrl(),URL);
    assertEquals(met.getIp(),IP);
    assertEquals(met.getDate(),DATE_TIME);
    assertEquals(met.getMimetype(),MIME_TYPE);
    assertEquals(met.getLength(),LENGTH);
    assertEquals(met.getOffset(),OFFSET);
  }
  /**
   * Verify that readAll() returns the given object in its entirity.
   * Tests bug #11.
   */
  public void testReadAll(){
    byte[] output = null;
    try
    {
      output = sar.readAll();
    }
    catch (IOException e)
    {
      fail(e.toString());
    };
    assertEquals(LENGTH, output.length);
    assertTrue(Arrays.equals(correctContent, output));
  }
  /**
   * Verify that getObjectAsInputStream() returns a stream that represents
   * the given object in its entirity.
   */
  public void testGetObjectAsInputStream() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
          InputStream is = sar.getObjectAsInputStream();
          int ch = is.read();
          while(ch >=0){
              baos.write(ch);
              ch = is.read();
          }
          is.close();
      }
      catch (IOException e) {
          fail(e.toString());
      }
      byte[] output = baos.toByteArray();
      assertEquals("Should get right length output", LENGTH, output.length);
      assertEquals("Should get correct output",
              new String(correctContent), new String(output));

  }

    public void testGetObjectAsInputStreamChunked() throws IOException {
        // Check that reading chunks also works
        testFile = new File(TestInfo.ORIGINALS_DIR, "2-metadata-1.arc");
        arcReader = ARCReaderFactory.get(testFile);
        testRec = (ARCRecord) arcReader.get(143);
        //TODO: Construction of sar should be put in each method? What if it fails?
        sar = new ShareableARCRecord(testRec, testFile);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[4096];
            InputStream is = sar.getObjectAsInputStream();
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1) {
                baos.write(buf, 0, bytesRead);
               // baos.write(buf);
            }
            is.close();
        }
        catch (IOException e) {
            fail(e.toString());
        }
        byte[] output = baos.toByteArray();
        // Copy of record data
        File f = new File(TestInfo.ORIGINALS_DIR, "2-metadata-1.arc-contents");
//        assertEquals("Should get right length output", f.length(), output.length);
        assertEquals("Should get right output",
                FileUtils.readFile(f), new String(output));
    }

  /**
   * Verify that several readers can request the object as both byte array
   * and InputStream and still get correct and independent representations
   * - in particular when position is changed in the InputStreams and
   *   the byte arrays are manipulated.
   */
  public void testSharing()
  {
    try
    {
      InputStream is = sar.getObjectAsInputStream();
      is.read();
      byte[] bar = sar.readAll();
      for(int i=0;i<LENGTH;i++)
      {
        bar[i] = (byte)(i % 42);
      }
      is.close();
      testReadAll();
      testGetObjectAsInputStream();
    }
    catch (IOException e)
    {
      fail(e.toString());
    } catch (IndexOutOfBoundsException e) {
        fail("Shouldn't throw " + e);
    }
  }

}