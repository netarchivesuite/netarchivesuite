
import java.util.Set;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;


public class ShowListOfJobsForDeduplicationIndex {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Long harvestId = Long.getLong(args[0]);
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        Set<Long> jobSet = dao.getJobIdsForSnapshotDeduplicationIndex(harvestId);
        System.out.println("# jobs used in deduplication index for harvest # " + harvestId + " is " + jobSet.size());
        for(Long id: jobSet){
            System.out.println(id);
        }

    }

}
