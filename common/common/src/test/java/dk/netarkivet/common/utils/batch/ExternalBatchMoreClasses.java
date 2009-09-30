/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * A batch job which returns following statistical information about all files
 * in the bitarchive in which it runs:
 *  - short form of metadata filename
 *  - date of creation taken from arc-file header
 *  - date taken from cdx part
 *  - date taken from lines starting on form YYYY-MM-DDT
 *  - date taken from process report line
 *  - section with statistical data taken from the arc-file consisting of 
 *    <#urls> <#bytes> <mime-types>
 *    
 *    
 *    This is the class which is used to generate the two jarfiles
 *    ExternalBatchSeveralClassesNoPackage.jar
 *    ExternalBatchSeveralClassesWithPackage.jar
 */

public class ExternalBatchMoreClasses extends FileBatchJob {
    protected transient Log log = LogFactory.getLog(getClass().getName());

    /**
     * Initializes fields in this class.
     * @param os the OutputStream to which data is to be written
     */
    public void initialize(OutputStream os) {

    }

    /**
     * Invoke default method for deserializing object, and reinitialise the
     * logger.
     * @param s
     */
    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during deserialization", e);
        }
        log = LogFactory.getLog(getClass().getName());
    }

    /**
     * Type to indicate where date was found in metadata-file 
     * 0. filedesc, 1. cdx, 2. on form YYYY-MM-DDT, 3. Processors report
     */    
    private static enum DateFoundType {FIRSTLINE, CDX, FORMATYMD, PROCREPORT};
    
    /**
     * Type to include both a date and where it was found in metadata-file 
     */    
    private static class DateAndFoundType {
        String date = "";
        int foundType = -1; //corresponding to DateFoundType ordinal 
    };
    
    /**
     * Checks whether a string only contains digits
     *  will return true
     * @param s String to be checked
     * @return true if the given string only contains digits (or is an empty 
     *         line), false otherwise
     */    
	private static boolean isStringNumeric(String s) {
		boolean ok = true;
		int l = s.length();
		int i = 0;
		while (i < l && ok) {
			char c = s.charAt(i);
			ok = Character.isDigit(c);
			i++;
		}
		return ok;
	}

	/**
     * Remove all extra occurrances in a String, i.e. a " " will only occur 
     * once at a time and there are no start or end space.
     * @param line to be trimmed for spaces
     * @return string as result of trimmed line
     */    
	private String extractTrimedLine(String line) {
		//split on " " and ignore empty strings
    	String[] parts = line.split(" ");
    	String res = "";
		for (int i=0; i < parts.length; i++) {
			if (parts[i].length() != 0) {
				res = (res.length() == 0 ? "" : res + " ") + parts[i] ;
			} // else ignore
		}
		return res;
	}

    /**
     * Looks for date in given line. This can either be 
     *  - a date of creation taken from arc-file header
     *  - date taken from cdx part
     *  - date taken from lines starting on form YYYY-MM-DDT
     *  - date taken from process report line
     *  If a date is found then the given date array is updated
     *  The return value tells whether a date was found.
     * @param line to be searched
     * @param firstLine true if it is the first line of the metadata-file
     * @param 
     * @return true if date was found, false otherwise
     */    
	public static DateAndFoundType lookForDate(
               String line, String mdFile, boolean firstLine) {
        
        DateAndFoundType resDateFound = new DateAndFoundType();
        resDateFound.date = ""; 
        resDateFound.foundType = -1; 

		if (firstLine) {
            // Find date from first line which is on form: 
            // filedesc://<filname> <#.#.#.#> YYYYMMDDHHMMSS ... 
	    	if (line.indexOf("filedesc://") == 0) {
	    		int i = line.indexOf(" ", 8); //find 1. " "
	    		if (line.length() > i + 1) { 
	    			i = line.indexOf(" ", i + 1); //find 2. " " 
	    	    }
	    		if (line.length() >= i + 1 + 14) { 
                    //Check whether the date consist of digits 
                    String s = line.substring(i + 1, i + 9);
                    if (isStringNumeric(s) && (s.length() > 0)) {
                        //Check whether it is a metadata-2 file, meaning that the date is
                        //not trustworthy
                        if (mdFile.indexOf("-2") < 0) {
                            resDateFound.date = s;
                            resDateFound.foundType = DateFoundType.FIRSTLINE.ordinal(); 
                        }
                    }
	    		}
	        } 
		} else {
	    	//cdx -line on form 
            //metadata://netarkivet.dk/crawl/index/cdx"
            if (line.indexOf("metadata://netarkivet.dk/crawl/index/cdx") >= 0) {
				int pos = line.indexOf("timestamp=");
				if (pos > 0 && 
                    line.length() >= pos + 14 + "timestamp=".length() + 1) {
                    resDateFound.date = 
                        line.substring(pos + "timestamp=".length(), 
                                       pos + 8 + "timestamp=".length());
                    resDateFound.foundType = DateFoundType.CDX.ordinal(); 
				}
			}
	    	//on form YYYY-MM-DDT...
			if (line.indexOf("http://") > 0) {
				if (line.indexOf("-")==4 && line.indexOf("T")==10 && line.length()>"2007-04-09T".length()) {
	    			String year = line.substring(0, 4); 
	    			String month = line.substring(5, 7); 
	    			String delim = line.substring(7, 8); 
	    			String day = line.substring(8, 10); 
	    			if (isStringNumeric(year) &&
	    				isStringNumeric(month) &&
	    				isStringNumeric(day) &&
	    				delim.equals("-") 
	    			) {
                        resDateFound.date = year + month + day; 
                        resDateFound.foundType = DateFoundType.FORMATYMD.ordinal();
	    			}
				}
			}
			//Processors report
			int pos = line.indexOf("Processors report - ");
			if (pos >= 0 && line.length() >= pos+12) {
                resDateFound.date =  
                    line.substring(pos + "Processors report - ".length(), 
                                   pos + "Processors report - ".length() + 8);
                resDateFound.foundType = DateFoundType.PROCREPORT.ordinal(); 
			}
		}
		return resDateFound;
	}

	/**
     * Collects statistical information about files from a metadata arc file
     * and dates from different parts of the metadatafile.
     * @param file processing file
     * @return string with dates and extracted statistical information
     */    
    private String readStatInfoFromFile(File file, String mdFileName) {
        /* File to read */
        BufferedReader in = null;
       
        /* Array of read dates on 8 characters (YYYYMMDD).
        /* Each array position indicates how the date was found 
         * (according to type DateFoundType) 
         * 0. in header, 1. cdx, 2. on form YYYY-MM-DDT, 3. Processors report
         */
        String[] fileDates = new String[4];
        
        /* String with read statistical information */
        String statInfo = "";
        
        
        fileDates[DateFoundType.FIRSTLINE.ordinal()] = "";
        fileDates[DateFoundType.CDX.ordinal()] = "";
        fileDates[DateFoundType.FORMATYMD.ordinal()] = "";
        fileDates[DateFoundType.PROCREPORT.ordinal()] = "";

        try {
            try {
	            in = new BufferedReader(new FileReader(file));
	            String line;

	            /* Look for date on first line of arc-file */
	            if ((line = in.readLine()) != null) {
                    DateAndFoundType dateFound = lookForDate(line, mdFileName, true);
                    if (dateFound.foundType >= 0) {
                        fileDates[dateFound.foundType] = dateFound.date;
                    }
				}
				
	            /* Read heritrix stat data from arc-file where data is 
	        	 * found in file section beginning with a heading that contains 
	        	 * the text '[mime-types]' and [#urls], as f.ex. 
	        	 *   [#urls] [#bytes] [mime-types] or 
	        	 *   [mime-types]      [#urls]    [#bytes] 
	        	 * The section continues with lines with sets of values and 
	        	 * ending with an empty line or line not split into more than 
	        	 * two parts. Note that mime-types can include spaces.
	        	 * 
                 * Furthermore try to look for date elsewhere 
	        	 */
	            Boolean goOn = true;
	            while ((goOn && (line = in.readLine()) != null)) {
	            	//look for date 
                    DateAndFoundType dateFound = lookForDate(line, mdFileName, false);
                    if (dateFound.foundType >= 0) {
                        fileDates[dateFound.foundType] = dateFound.date;
                    }
                    
	            	//look for header of statistical data
	                if (line.indexOf("[mime-types]") >= 0 && line.indexOf("[#urls]") >= 0) {
	    	        	String trimLine = extractTrimedLine(line);
						
	            		//set positions of data according to header
						String[] lineParts = trimLine.split(" ");
						int mimePos = 0;
	    	        	int noFields = lineParts.length;
	    	        	String header = "";
						if (noFields != 3) {
							header = "Header do not contain 3 header items: '" + line + "'\n"; 
						} else {
							for (int i = 0; i<3; i++) {
								if (lineParts[i].equalsIgnoreCase("[#urls]") ||
									lineParts[i].equalsIgnoreCase("[#bytes]") ||
									lineParts[i].equalsIgnoreCase("[mime-types]")) {
									
                                    if (lineParts[i].equalsIgnoreCase("[mime-types]")) {
										mimePos = i;
									}
								} else {
									header = "Header do not contain expected headers (unknown name): '" + line + "'\n"; 
								}
							}
							if (header.length() == 0) {
								header = trimLine;
							}
						}
		            	statInfo = statInfo + header + "\n"; 
	            		
	            		//read rest of stat data
	                    while ((goOn && (line = in.readLine()) != null)) {
    	    				String statLine = "";
	    	    			trimLine = extractTrimedLine(line);
		    	    		lineParts = trimLine.split(" ");

	    	    			if (trimLine.length() == 0) {
	    	    				statLine = "\n";
	    	    				goOn = false;
	    	    			} 
							if (noFields != 3 && statLine.length() == 0) {
								//not seen in first run, but we cannot handle 
								//special cases, because we do not know the types
		    	    			if (noFields != lineParts.length) {
		    	    				goOn = false;
		    	    				statLine = "Ended with non-empty line (no colums !=3): '" + line + "'\n"; 
		    	    			} else {
									statLine  = trimLine + "\n"; 
		    	    			}	
							} 
							if (lineParts.length == 1 && statLine.length() == 0) {
								//we do not know what is going on
	    	    				statLine = "Ended with non-empty line (expected 3 found 1): '" + line + "'\n"; 
							}
							if (lineParts.length == 3 && statLine.length() == 0) {
								//all in order
								statLine  = trimLine + "\n"; 
							}
							if (lineParts.length == 2 && statLine.length() == 0) {
								//there may be null mimetypes
								//check that the 2 contains numbers
								boolean ok = isStringNumeric(lineParts[0]);
								ok = ok && isStringNumeric(lineParts[1]);
								
								if (ok) {
									String mime = "<blank-mime>";
									boolean mimePassed = false;
									for (int i = 0; i<3; i++) {
										if (i == mimePos) {
											statLine = statLine + mime + " ";
											mimePassed = true;
										} else {
											statLine = statLine + lineParts[i - (mimePassed?1:0)] + " ";
										}
									}
									statLine  = statLine.substring(0, statLine.length()-1) + "\n"; 
								} else {
									//we cannot recognize it
									statLine = "Ended with non-empty line (expected 2 numbers): '" + line + "'\n";
								}
							}
							if (lineParts.length > 3 && statLine.length() == 0) {
								//there may be mimetypes with spaces
								boolean ok = true;
								if (mimePos == 0) {
									ok = ok && isStringNumeric(lineParts[lineParts.length-1]);
									ok = ok && isStringNumeric(lineParts[lineParts.length-2]);
								} else {
									if (mimePos == 1) {
										ok = ok && isStringNumeric(lineParts[lineParts.length-1]);
										ok = ok && isStringNumeric(lineParts[0]);
									} else {
										if (mimePos == 2) {
											ok = ok && isStringNumeric(lineParts[0]);
											ok = ok && isStringNumeric(lineParts[1]);
										}
									}
								}
								if (ok) {
									if ((mimePos == 1) || (mimePos == 2)) {
										statLine = statLine + " " + lineParts[0];
										if (mimePos == 2) {
											statLine = statLine + " " + lineParts[1];
										}
									}
									int mimeEndPos = 0;
									mimeEndPos = lineParts.length - 3 + mimePos;
									for (int i = mimePos; i < mimeEndPos; i++) {
										statLine = statLine + (i==mimePos?"":"###") + lineParts[i];
									}
									if (mimePos == 0) {
										statLine = statLine + " " + lineParts[lineParts.length-2];
										statLine = statLine + " " + lineParts[lineParts.length-1];
									}
								} else {
									//we cannot recognize it
									statLine = "Ended with non-empty line (expected numbers in 2 where mimepos was '" + mimePos + "'): '" + line + "'\n";
								}
							}
		                	if (statLine.length() == 0) {
								statLine = "Ended with non-empty line (nothing was calculated - internal error): '" + line + "'\n";
		                	}
							statInfo = statInfo + statLine; 
	                	} //while read next line of statlines
					} //header
               	}
            } finally {
                if (in != null) {
                    in.close();
            	}
            } 
        } catch (IOException e) {
            String msg = "Could not read data from "
                         + file.getAbsolutePath();
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
		
        /* Log (and write if unexpected data occured */
        String fileDateTxt = ""; 
        boolean dateFound = false;
        for (int i=DateFoundType.FIRSTLINE.ordinal(); i<=DateFoundType.FIRSTLINE.ordinal(); i++) {
            dateFound = dateFound || (fileDates[i].length() > 0);
        }
        if (dateFound) {
            fileDateTxt = fileDates[DateFoundType.FIRSTLINE.ordinal()] 
                + "," + fileDates[DateFoundType.CDX.ordinal()]
                + "," + fileDates[DateFoundType.FORMATYMD.ordinal()]
                + "," + fileDates[DateFoundType.PROCREPORT.ordinal()];
        } else {
    		fileDateTxt = "Could not read arc file date from " 
    			       + file.getAbsolutePath();
        }
        
        if (statInfo.length() == 0) {
        	statInfo = "Could not read statistics from " 
    			        + file.getAbsolutePath() + "\n";
        }
        
		/* return result */
        return fileDateTxt + "\n" + statInfo;
    }

    /**
     * Writes file, date and statistical data from a metadata arcfile to the 
     * OutputStream. This data will be on form:
     *  <metadata arc file name>,<date0>,<date1>,<date2>,<date3>
     *  <section with statistical data taken from the arc-file>
     *  ending with and empty line.
     * Here 
     * - <metadata arc file name> is on form <job no.>"-metadata-"<no.>".arc"
     * - <datei> are dates on form YYYYMMDD, found: 
     *   0. in header, 1. cdx, 2. on form YYYY-MM-DDT, 3. Processors report
     *   if no date where found, it will be represented by the empty string.
     * - <section with statistical data taken from the arc-file> starts with:
     *    <#urls> <#bytes> <mime-types>
     *   and is followed by numbers and tekst according to theis header.
     * @param file an arcfile
     * @param os the OutputStream to which data is to be written
     * @return false If listing of this arcfile fails; otherwise true
     */
    public boolean processFile(File file, OutputStream os) {
        ArgumentNotValid.checkNotNull(file, "file");
        
        String result = "";
        String mdFileName = "";
        
        // Read arc file name
        String name = file.getName();  //arc file name

        // Check it is a metadata file
        if (name.indexOf("metadata") == -1) {
        	return true; //ignore
        } else {
        	mdFileName = name.replace("metadata-", "");
        	mdFileName = mdFileName.replace(".arc", "");
        }
        
        // Read statistics from metadata arc file
        result = mdFileName + "," + readStatInfoFromFile(file, mdFileName) + "\n";
        
        //Write result on output stream
        try {
            os.write(result.getBytes());
        } catch (IOException e) {
            log.warn("File stat info " + file.getName() + ", ... " 
            		                   + " failed: ", e);
            return false;
        }
        return true;
    }

    /**
     * Does nothing.
     * @param os the OutputStream to which data is to be written
     */
    public void finish(OutputStream os) {
    }

    public String toString() {
        int n_failed;
        if (filesFailed == null) {
            n_failed = 0;
        } else {
            n_failed = filesFailed.size();
        }
        return ("\nFileList job:\nFiles Processed = "
                + noOfFilesProcessed
                + "\nFiles  failed = " + n_failed);
    }
}
