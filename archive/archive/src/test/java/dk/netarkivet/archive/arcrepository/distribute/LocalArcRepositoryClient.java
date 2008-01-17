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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.io.IOException;

import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.archive.bitarchive.BitarchiveARCFile;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.IOFailure;

public class LocalArcRepositoryClient extends JMSArcRepositoryClient {

    private File theArcFile;


    public LocalArcRepositoryClient(File arcFile) {
        super();
        theArcFile = arcFile;
        }

    public BitarchiveRecord get(String arcFileName, long index) {
        if (!theArcFile.getName().equals(arcFileName)) {
            System.out.println(arcFileName + " not found");
            return null;
        }
        File arcFile = new File(arcFileName);
        if (!arcFile.exists()) {
            System.out.println("File not found: " + arcFileName);
            return null;
        }
        BitarchiveARCFile barc = new BitarchiveARCFile(arcFileName, arcFile);
        if (barc == null) {
            return null;
        }
        ARCReader arcReader = null;
        ARCRecord arc = null;
        try {
            if ((barc.getSize() <= index) || (index < 0)) {
                String s = "GET: index out of bounds: " + arcFileName + ":" + index
                    + " > " + barc.getSize();
                System.out.println(s);
                return null;
            }
            File in = barc.getFilePath();
            arcReader = ARCReaderFactory.get(in);
            arc = (ARCRecord) arcReader.get(index);
            BitarchiveRecord result = new BitarchiveRecord(arc);

            // release resources locked
            log.info("GET: Got " + result.getLength()
                     + " bytes of data from " + arcFileName + ":" + index);
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            //
            // }
            return result;
        } catch (IOException e) {
            final String msg = "Could not get data from " + arcFileName + " at: "
                    + index + "; Stored at: " + barc.getFilePath();
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (IndexOutOfBoundsException e) {
            final String msg = "Could not get data from " + arcFileName + " at: "
                    + index + "; Stored at: " + barc.getFilePath();
            log.warn(msg,e );
            throw new IOFailure(msg, e);
        } finally {
            try {
                if (arc != null) {
                    arc.close();
                }
                if (arcReader != null) {
                    arcReader.close();
                }
            } catch (IOException e) {
                log.warn("Could not close ARCReader or ARCRecord: ", e);
            }
        }
    }

}
