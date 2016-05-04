package dk.netarkivet.harvester.heritrix3.report;

import java.io.File;
import java.io.FilenameFilter;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * 
 * org.archive.jbs.Parse expects warc.gz files, and generates Hadoop sequence files with a json record for each processed url that is not skipped
 * TODO using the log printed to stdout we can make a list of records skipped, and records processed
 */
public class GenerateTextExtracts {

	public static void doTextExtracts(File warcFilesDir) {
	    ArgumentNotValid.checkExistsDirectory(warcFilesDir, "File warcFilesDir");
	    File[] warcfiles = warcFilesDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.contains("warc")) {
					return true;
				}
				return false;
			}
		});
	    
	    // Make an process that generates export DESTDIR=$1

//	    export SOURCEFILE=$2
//	    		if [ -z "$1" ]
//	    		  then
//	    		    echo "No DESTDIR argument supplied"
//	    		    exit
//	    		fi
//	    		if [ -z "$2" ]
//	    		  then
//	    		    echo "No SOURCEFILE argument supplied"
//	    		    exit
//	    		fi
//	    		TIMESTAMP=`date +'%Y%m%d%H%M'`
//	    		echo "Calling org.archive.jbs.Parse on ${SOURCEFILE}. DESTDIR is ${DESTDIR}" > seq-parsing-$TIMESTAMP.log
//	    		hadoop jar jbs-fatjar.jar org.archive.jbs.Parse $DESTDIR $SOURCEFILE >> seq-parsing-$TIMESTAMP.log 2>&1

	    
	    
    }

}
