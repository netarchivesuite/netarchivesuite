import java.util.Iterator;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.harvester.dao.DomainDAO;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;


public class TestSnapShotHarvestDomainConfigRetrieval {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Testing first step snapshot harvest" +  new Date());
        DomainDAO dao = DomainDAO.getInstance();

        Iterator<DomainConfiguration> i = new FilterIterator<Domain, DomainConfiguration>(
                dao.getAllDomainsInSnapshotHarvestOrder()) { 
            public DomainConfiguration filter(Domain domain) {
                if (domain.getAliasInfo() == null
                        || domain.getAliasInfo().isExpired()) {
                    return domain.getDefaultConfiguration();
                } else {
                    return null;
                }
            }
        };
        System.out.println("Iterator initialized " + new Date());
        long count = 0;
        List<DomainConfiguration> subset = new ArrayList<DomainConfiguration>();
        while (i.hasNext()) {
            count++;
            if (subset.size() >= 10000) {
                subset = new ArrayList<DomainConfiguration>();
                System.out.println("collected " + subset.size() + "  configs at " + new Date());
            }
            DomainConfiguration dc = i.next();
            subset.add(dc);
            if (count % 100 == 0) {
                System.out.println("#" + count + ": (conf-name, domain)= (" + dc.getName() 
                        + ", " + dc.getDomainName() + ") at " + new Date());
            }
        }
    }
}
