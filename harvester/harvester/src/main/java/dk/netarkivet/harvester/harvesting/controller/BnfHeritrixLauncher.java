/* File:       $Id: BnfHeritrixLauncher.java 752 2009-03-05 18:09:21Z svc $
 * Revision:   $Revision: 752 $
 * Author:     $Author: svc $
 * Date:       $Date: 2009-03-05 19:09:21 +0100 (to, 05 mar 2009) $
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
package dk.netarkivet.harvester.harvesting.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;

/**
 * @author ngiraud
 *
 */
public class BnfHeritrixLauncher extends HeritrixLauncher {
	
	/** The class logger. */
    final Log log = LogFactory.getLog(getClass());
	
	/** The CrawlController used. */
    private BnfHeritrixController heritrixController;
	
	private BnfHeritrixLauncher(HeritrixFiles files) throws ArgumentNotValid {
		super(files);
	}
	
	/**
     * Get instance of this class.
     *
     * @param files Object encapsulating location of Heritrix crawldir and
     *              configuration files
     *
     * @return {@link DefaultHeritrixLauncher} object
     *
     * @throws ArgumentNotValid If either order.xml or seeds.txt does not exist,
     *                          or argument files is null.
     */
    public static BnfHeritrixLauncher getInstance(HeritrixFiles files)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        return new BnfHeritrixLauncher(files);
    }

	public void doCrawl() throws IOFailure {
    	setupOrderfile();
        heritrixController = new BnfHeritrixController(getHeritrixFiles());
        try {
            // Initialize Heritrix settings according to the order.xml
            heritrixController.initialize();
            log.debug("Starting crawl..");
            heritrixController.requestCrawlStart();
            
            while (true) {
            	
            	// First we wait the configured amount of time        	
            	waitSomeTime();
            	
            	CrawlProgressMessage cpm;        	
            	try {
            		cpm = heritrixController.getCrawlProgress();
            	} catch (IOFailure iof) {
            		// Log a warning and retry
            		log.warn("IOFailure while getting crawl progress", iof);
            		continue;
            	}
            	
            	getJMSConnection().send(cpm);        	
            	
            	if (cpm.crawlIsFinished()) {
            		// Crawl is over, exit the loop
            		break;
            	}
            	
            	HeritrixFiles files = getHeritrixFiles();
            	log.info("Job ID: " + files.getJobID()
                         + ", Harvest ID: " + files.getHarvestID()
                         + ", " + cpm.getHostUrl()
                         + "\n"
                         + cpm.getProgressStatisticsLegend() + "\n"
                     	 + cpm.getJobStatus().getStatus() 
                     	 + " " + cpm.getJobStatus().getProgressStatistics());        	
                
            }
            
        } catch (IOFailure e) {
            log.warn("Error during initialisation of crawl", e);
            throw (e);
        } catch (Exception e) {
            log.warn("Exception during crawl", e);
            throw new RuntimeException("Exception during crawl", e);
        } finally {        	
        	if (heritrixController != null) {
                heritrixController.cleanup(getHeritrixFiles().getCrawlDir());
            }
        }
        log.debug("Heritrix is finished crawling...");
    }

}
