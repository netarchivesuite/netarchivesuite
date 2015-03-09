package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;

import org.junit.Test;

import dk.netarkivet.common.exceptions.IllegalState;

public class H3HeritrixTemplateTester {

	String basicArchiveFilePrefix = "netarkivet-1-1";
	String correctTemplateName = "crawler-beans_with_placeholders.cxml";
	String incorrectTemplateName = "crawler-beans_no_placeholders.cxml";
	@Test
	public void testArchiveFilePrefixSetter() {
		URL url = this.getClass().getClassLoader().getResource("heritrix3");
		assertFalse("Link til Heritrix3 ressources is broken", url == null);
	    File basedir = new File(url.getFile());
	    //System.out.println("Heritrix3-ressources found at: " + file.getAbsolutePath());
	    File beansWithNoPlaceholders = new File(basedir, incorrectTemplateName);
	    File beansWithPlaceholders = new File(basedir, correctTemplateName);
	    
		HeritrixTemplate tBad = HeritrixTemplate.read(beansWithNoPlaceholders);
		HeritrixTemplate tOk = HeritrixTemplate.read(beansWithPlaceholders);
		assertTrue(tOk instanceof H3HeritrixTemplate);
		assertTrue(tBad instanceof H3HeritrixTemplate);
		try {
			H3HeritrixTemplate h3Template = (H3HeritrixTemplate) tBad;
			h3Template.setArchiveFilePrefix(basicArchiveFilePrefix);
			fail("Should have thrown IllegalState on missing placeholder");
		} catch (IllegalState e) {
			// Expected
		}
		
		try {
			H3HeritrixTemplate h3Template = (H3HeritrixTemplate) tOk;
			h3Template.setArchiveFilePrefix(basicArchiveFilePrefix);
		} catch (IllegalState e) {
			e.printStackTrace();
			fail("Shouldn't have thrown IllegalState with placeholder available");
		}
	}

}
