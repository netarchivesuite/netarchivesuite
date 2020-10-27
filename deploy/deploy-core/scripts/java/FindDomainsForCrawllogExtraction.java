import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;


public class FindDomainsForCrawllogExtraction {

    /**
     * @param args
     * @throws SQLException 
     */
    public static void main(String[] args) throws SQLException {
        if (args.length != 6) {
            System.out.println("Missing args. Need 6. Only got " + args.length);
            System.exit(1);
        }
        String harvestOneName = args[0];
        String harvestTwoName = args[1];
        String harvestThreeName = args[2];
        String harvestFourName = args[3];
        int minObjectCount = Integer.parseInt(args[4]);
        int numDomainsToSelect = Integer.parseInt(args[5]);
        Long harvestOneId = 
                HarvestDefinitionDAO.getInstance().getHarvestDefinition( harvestOneName).getOid();
        Long harvestTwoId = 
                HarvestDefinitionDAO.getInstance().getHarvestDefinition( harvestTwoName).getOid();
        Long harvestThreeId = 
                HarvestDefinitionDAO.getInstance().getHarvestDefinition( harvestThreeName).getOid();
        Long harvestFourId = 
                HarvestDefinitionDAO.getInstance().getHarvestDefinition( harvestFourName).getOid();
            
        // Establish connection to database (use settings.xml)

        //select config_id, job_id, bytecount from historyinfo where harvest_id = 158 and stopreason = 0 
        //        and bytecount > 0 order by bytecount ASC limit 20;
               
        
        Map<Long, ConfigEntry> harvestOneMap = new HashMap<Long, ConfigEntry>();
        Map<Long, ConfigEntry> harvestTwoMap = new HashMap<Long, ConfigEntry>();
        Map<Long, ConfigEntry> harvestThreeMap = new HashMap<Long, ConfigEntry>();
        Map<Long, ConfigEntry> harvestFourMap = new HashMap<Long, ConfigEntry>();
        
        findDataForHarvestId(harvestOneId, minObjectCount, harvestOneMap);
        findDataForHarvestId(harvestTwoId, minObjectCount, harvestTwoMap);
        findDataForHarvestId(harvestThreeId, minObjectCount, harvestThreeMap);
        findDataForHarvestId(harvestFourId, minObjectCount,harvestFourMap);
        
        Set<Long> entries = new HashSet<Long>();
        for (Long id: harvestOneMap.keySet()) {
            if (harvestTwoMap.containsKey(id) && harvestThreeMap.containsKey(id) 
                    && harvestFourMap.containsKey(id)) {
             entries.add(id);
             if (entries.size() >= numDomainsToSelect) {
                 break;
             }
            }
        }
        
        System.err.println("Found " + entries.size() + " candidate domains");
        
        System.out.println("################################################");
        System.out.println("Domainnavn (configName):  JOB_ID-1 JOB_ID-2 JOB_ID-3 JOB_ID-4");
        System.out.println("################################################");
        for (Long id: entries) {
            DomainEntry dEntry= findDomainEntryFromConfigId(id);
            System.out.print("'" + dEntry.dName + "' ('" + dEntry.cName + "': ");
            printJobsInfo(harvestOneMap, id); System.out.print(",");
            printJobsInfo(harvestTwoMap, id); System.out.print(",");
            printJobsInfo(harvestThreeMap, id); System.out.print(",");
            printJobsInfo(harvestFourMap, id); System.out.println("");
        }
        System.out.println("################################################");
    }
    
    private static void printJobsInfo(Map<Long,ConfigEntry> cMap, long id) {
        System.out.print(cMap.get(id).jId + " (oc=" + cMap.get(id).oCount + ", bc=" 
                + cMap.get(id).bCount + ")");
    }
    
    
    
    private static DomainEntry findDomainEntryFromConfigId(Long id) throws SQLException {
        Connection conn =  HarvestDBConnection.get();
        PreparedStatement s = DBUtils.prepareStatement(conn, 
                "SELECT name, domain_id FROM configurations where config_id = ?", id);
        ResultSet res = s.executeQuery();
        DomainEntry entry = null;
        while (res.next()) {
            entry = new DomainEntry(
                    id, res.getString(1), getDomainName(res.getLong(2)));
//            if (!res.last()) {
//             System.out.println("Should be only one result");   
//            }
        } 
        HarvestDBConnection.release(conn);
        return entry;
        
    }

    private static String getDomainName(long id) throws SQLException {
        Connection conn = HarvestDBConnection.get();
        PreparedStatement s = DBUtils.prepareStatement(conn, 
                "SELECT name FROM domains WHERE domain_id = ?", id);
        String res = DBUtils.selectStringValue(s);
        HarvestDBConnection.release(conn);
        return res;
    }

    private static void findDataForHarvestId(long harvestId, long minObjectCount, Map<Long, ConfigEntry> map) 
            throws SQLException {
        try (Connection conn =  HarvestDBConnection.get(); ) {
            PreparedStatement s = DBUtils.prepareStatement(conn,
                    "SELECT config_id, job_id, bytecount, objectcount from historyinfo " +
                            "where stopreason=0 and harvest_id=? and objectcount > ?", harvestId, minObjectCount);
            ResultSet res = s.executeQuery();
            while (res.next()) {
                ConfigEntry entry = new ConfigEntry(
                        res.getLong(1), res.getLong(2), res.getLong(3), res.getLong(4));
                map.put(entry.getcId(), entry);
            }
        }
    }
    
    
    
    
    private static class ConfigEntry {
        private long cId;
        private long jId;
        private long bCount;
        private long oCount;
        public ConfigEntry(long configId, long jobId, long bytecount, long objectCount) {
            cId = configId;
            jId = jobId;
            bCount = bytecount;
            oCount = objectCount;        }

        public long getcId() {
            return cId;
        }
    }
    
    private static class DomainEntry {
        private long cId;
        private String cName;
        private String dName;
  
        public DomainEntry(long configId, String configName, String domainName) {
            cId = configId;
            cName = configName;
            dName = domainName;
        }

       
    }

}
