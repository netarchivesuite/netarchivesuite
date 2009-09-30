/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.externalsoftware;

import is.hi.bok.deduplicator.CrawlDataItem;
import is.hi.bok.deduplicator.CrawlDataIterator;
import is.hi.bok.deduplicator.CrawlLogIterator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

public class CheckDuplicateReduction {
    
    /**
     * Utility to validate Deduplication.
     * Uses a crawl.log and a collection of arc-files
     * 
     * Parses the crawl.log and looks for duplicate entries like this:
     * "2006-07-31T13:26:12.687Z   200  
     * 428 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_07.gif E 
     * http://netarkivet.dk/index-en.php image/gif #044 20060731112612682+3 Q6TITNTYNWCP3BQIS7L5X7GQPP5FBI3F - 
     * duplicate:"2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc,84231"
     * @param crawlLog
     * @param arcfiles
     * @return true, if result is valid.
     * @throws Exception If unable to get absolute path for crawl.log
     * 
     */
     
     public static boolean checkDuplicationResult(File crawlLog, File[] arcfiles) throws Exception {
         CrawlDataIterator cdi = new CrawlLogIterator(crawlLog.getAbsolutePath());
         while (cdi.hasNext()) {
             CrawlDataItem cd = cdi.next();
             String originString = cd.getOrigin();
             // Separate arcfile and offset
             if (originString != null) {
                 String[] originParts = originString.split(",");
                 String arcFile = originParts[0];
                 long offset = Long.parseLong(originParts[1]);
                 System.out.println("url: " + cd.getURL());
                 System.out.println("arcfile: " + arcFile);
                 System.out.println("offset: " + offset);
                 // match arcfile with File in arcfiles
                 File matchedFile = null;
                 for (File thisFile: arcfiles){
                     if (thisFile.getName().equals(arcFile)) {
                         matchedFile = thisFile;
                     }
                 }
                 if (matchedFile == null) {
                     System.out.println("Arcfile was not found!!!!");
                     return false;
                 } else {
                     org.archive.io.arc.ARCRecord originARCrecord = getARCRecord(matchedFile, offset);
                     if (originARCrecord.getMetaData().getUrl().equals(cd.getURL())) {
                         // expected behaviour
                     } else {                         
                         return false;
                     }
                 }
             }
         }
         return true;
     }
    

    private static ARCRecord getARCRecord(File file, long offset) throws IOException {
        ARCReader ar = ARCReaderFactory.get(file);
        return (ARCRecord) ar.get(offset);
    }


    /**
     * @param args crawl.log; directory, where arcs are located
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Too few arguments. We need " + argsAsString());
            System.exit(1);
        }
        File arcsDir = new File(args[1]);
        boolean result = checkDuplicationResult(new File(args[0]),
                arcsDir.listFiles(new ARCFileFilter()));
        System.out.println("result: " + result);
        
    }
    
    public static String argsAsString(){
        return " <path crawl.log> <path to arcsdir>";
    }
    
    static class ARCFileFilter implements FilenameFilter {
        
        public boolean accept(File dir, String name) {
            return (name.endsWith(".arc"));
        }
    }
    
    

}
