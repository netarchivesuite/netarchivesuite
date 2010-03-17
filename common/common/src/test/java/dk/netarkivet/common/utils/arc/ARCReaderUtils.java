/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.cdx.CDXReader;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.cdx.CDXUtils;

/**
 * Utilities to extract contents from arcfiles.
 *
 */
public class ARCReaderUtils {

   static final String SEPARATOR_REGEX = "\\s+";
   private ARCReaderUtils() {}

   /**
    * Dump contents of an Arcfile to destinationDir.
    * @param destinationDir the directory, where the files are dumped
    * @param ArcFile
    * @param excludeFilter Don't dump any records with mimetypes matching this string
    * @throws IOFailure
    */
   public static void dumpARC(File destinationDir, File ArcFile, String excludeFilter) throws IOFailure {
       ArgumentNotValid.checkNotNull(destinationDir, "destinationDir");
       ArgumentNotValid.checkNotNull(ArcFile, "arcFile");

       try {
           // create an index of the arc-file

           File cdxFile = File.createTempFile("reader-utils", "");
           File cdxFileSorted = File.createTempFile("reader-utils", "");
           CDXUtils.writeCDXInfo(ArcFile, new FileOutputStream(cdxFile));
           FileUtils.makeSortedFile(cdxFile, cdxFileSorted);
           FileUtils.copyFile(cdxFileSorted, new File("/tmp/svc/index/"));
           CDXReader cdxReader = new CDXReader(cdxFileSorted);
           List<CDXRecord> records = getCdxRecords(cdxFileSorted);

           ARCReader arcReader = ARCReaderFactory.get(ArcFile);
           ARCRecord arc;
           for (CDXRecord record:  records) {
                   System.out.println(
                           String.format("Dumping uri '%s' with mimetype %s",
                                   record.getURL(),
                                   record.getMimetype())
                                   );
                   String filename = getFilename(record.getURL());
                   if (filename == null) {
                       filename = "null";
                   }
                   ARCKey key = cdxReader.getKey(record.getURL());
                   if (key == null) {
                       System.err.println("Key not found for uri: " + record.getURL());
                       throw new IOFailure("Key not found for uri: " + record.getURL());
                   } else  {
                       arc = (ARCRecord) arcReader.get(key.getOffset());
                       arc.skipHttpHeader();
                       BitarchiveRecord result = new BitarchiveRecord(arc);

                       if (result == null) {
                           throw new IOFailure ("result not found");
                       }
                       InputStream is = result.getData();
                       File destination = new File(destinationDir, filename);
                       int counter = 0;
                       while (destination.exists()) {
                           destination = new File(destinationDir, filename + counter);
                           counter++;
                       }
                       copy(is, new FileOutputStream(destination));

                   }
               }


       } catch (IOException e) {
           throw new IOFailure("IOException thrown:" + e, e);
       }

   }

   private static String getFilename(String urlString){
   String[] urlpaths;
   try {
       URL url = new URL(urlString);
       urlpaths = url.getPath().split("/");

   } catch( Exception e) {
       return null;
   }
   return urlpaths[urlpaths.length -1];
}

/**
    * Generates a list of CDXrecords out of an cdxfile.
    * Assumes that the cdxfile is good quality.
    * @param cdxFile the given cdxfile
    * @return a list of CDXrecords out of an cdxfile.
    */
   private static List<CDXRecord> getCdxRecords(File cdxFile) {
       ArrayList<CDXRecord> records = new ArrayList<CDXRecord>();
       for (String line: FileUtils.readListFromFile(cdxFile)) {
           String[] field_parts = line.split(SEPARATOR_REGEX);
           records.add(new CDXRecord(field_parts));

       }
       return records;
   }

   public static void main(String[] args) throws IOException {
       dumpARC(new File("/tmp/svc"),
               new File("/home/svc/sheet-arcs/Sheetmusic-20041217122121540/arcs/SHEETMUSIC-20041217152908-00002-asterix.arc.gz")
               ,"^text/html");
   }
   /**
    * Copies the content of an InputStream to an OutputStream.
    * This method constructs an efficient buffer and pipes bytes from stream
    * to stream through that buffer.
    * The OutputStream is flushed after all bytes have been copied.
    *
    * @param content Source of the copy operation.
    * @param out Destination of the copy operation.
    */
    private static void copy(InputStream content, OutputStream out) {
     BufferedInputStream page = new BufferedInputStream(content);
     BufferedOutputStream responseOut = new BufferedOutputStream(out);
     try {
         byte[] buffer = new byte[4096];
         int bytesRead;
         while ((bytesRead = page.read(buffer)) != -1) {
             responseOut.write(buffer, 0, bytesRead);
         }
         responseOut.flush();
     } catch (IOException e) {
          throw new IOFailure("Could not read or write data", e);
     }
    }
}
