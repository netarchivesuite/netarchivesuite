package dk.netarkivet.harvester.utils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlEntityResolver;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlErrorHandler;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlValidationResult;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlertrapsUtils {
	
    protected static final Logger log = LoggerFactory.getLogger(CrawlertrapsUtils.class);
    
    /**
     * Test one or more lines for being XML wellformed.
     * @param traps one or more strings
     * @return false if exception is thrown during validation or validation false; otherwise true
     */
	 public static boolean isCrawlertrapsWellformedXML(List<String> traps ) {
		 String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><values>";
		 StringBuilder sb = new StringBuilder();
		 sb.append(prefix);
		 for (String trimmedLine: traps) {
			 sb.append("<value>" + trimmedLine + "</value>");
		 }
		 String end = "</values>";
		 sb.append(end);

		 ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
		 try {
			 XmlValidator xmlValidator = new XmlValidator();
			 XmlEntityResolver entityResolver = null;
			 XmlErrorHandler errorHandler = new XmlErrorHandler();
			 XmlValidationResult result = new XmlValidationResult();
			 return xmlValidator.testStructuralValidity(bais, entityResolver, errorHandler, result);
		 } catch (Throwable t) {
			 log.debug("Error found during xml validation", t);
			 return false;
		 }

	 }
	 /**
	  * Test one line for being XML wellformed.
	  * @param line a line being tested for wellformedness
	  * @return false if exception is thrown during validation or validation false; otherwise true
	  */
	 public static boolean isCrawlertrapsWellformedXML(String line) {
		 List<String> oneElementList = new ArrayList<String>();
		 oneElementList.add(line);
		 return isCrawlertrapsWellformedXML(oneElementList);
	 }
}
