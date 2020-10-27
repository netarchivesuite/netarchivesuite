package dk.netarkivet.harvester.tools;

import java.util.List;

import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.utils.CrawlertrapsUtils;

/**
 * Checks DomainCrawltraps in the Domain table for validity.
 * usage: java -Dnetarkivet.settings.file=some-settings-file dk.netarkivet.harvester.tools.CheckDomainCrawltraps 
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
            boolean isWellFormed = CrawlertrapsUtils.isCrawlertrapsWellformedXML(traps);
            System.out.println("DomainCrawlertraps (" + traps.size() + ") for domain '" + d.getName() + "' is " 
            		+ (isWellFormed?"OK":"NOT OK"));
            if (!isWellFormed) { // Examine the traps individually
                baddomaincount++;
                for (String trap: traps) {
                    boolean isWellFormedTrap = CrawlertrapsUtils.isCrawlertrapsWellformedXML(trap);
                    if (!isWellFormedTrap) {
                        System.out.println("domain '" + d.getName() + "' has the not wellformed trap '" + trap + "'");
                        badTrapsCount++;
                    }
                }
            }
        }
        System.out.println("Examined " +  domaincount + " domains.");
        System.out.println("Domains with not wellformed traps: " +  baddomaincount);
        System.out.println("Found " +  badTrapsCount + " not wellformed traps");
    }
}