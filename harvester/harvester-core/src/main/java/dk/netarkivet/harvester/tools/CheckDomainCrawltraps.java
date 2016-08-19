package dk.netarkivet.harvester.tools;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlEntityResolver;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlErrorHandler;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlValidationResult;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlValidator;

import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;

/**
 * Checks DomainCrawltraps for validity
 * usage: java -Dnetarkivet.settings.file=some-settings-file CheckDomainCrawltraps 
 * 
 * @author svc
 *
 */
public class CheckDomainCrawltraps {

    public static void main(String[] args) {
        DomainDAO dao = DomainDAO.getInstance();
        List<String> domainNames = dao.getAllDomainNames();
        long domaincount=0;
        long baddomaincount=0;
        long badTrapsCount=0;
        for (String domainName: domainNames) {
            Domain d = dao.read(domainName);
            domaincount++;
            List<String> traps = d.getCrawlerTraps();
            boolean isWellFormed = isCrawlertrapsWellformedXML(traps);
            System.out.println("DomainCrawlertraps (" + traps.size() + ") for domain '" + d.getName() + "' is " + (isWellFormed?"OK":"NOT OK"));
            if (!isWellFormed) { // Examine the traps individually
                baddomaincount++;
                for (String trap: traps) {
                    boolean isWellFormedTrap = isCrawlertrapWellformedXml(trap);
                    if (!isWellFormedTrap) {
                        System.out.println("domain '" + d.getName() + "' has the not wellformed trap '" + trap + "'");
                        badTrapsCount++;
                    }
                    
                    
                }
            }
        }
        System.out.println("Examined " +  domaincount + " domains.");
        System.out.println("Domains with not wellformed traps: " +  baddomaincount);
        System.out.println("Found  " +  badTrapsCount + " not wellformed traps");
    }
    
    private static boolean isCrawlertrapWellformedXml(String line) {
        List<String> list = new ArrayList<String>();
        list.add(line);
        return isCrawlertrapsWellformedXML(list);
        
    }
    
    private static boolean isCrawlertrapsWellformedXML(List<String> lines ) {
        String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><values>";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (String trimmedLine: lines) {
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
            t.printStackTrace();
            return false;
        }
        
    }

}