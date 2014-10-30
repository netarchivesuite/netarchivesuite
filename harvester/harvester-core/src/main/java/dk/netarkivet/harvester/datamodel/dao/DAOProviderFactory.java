package dk.netarkivet.harvester.datamodel.dao;

import javax.inject.Provider;

import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;

public class DAOProviderFactory {
    public static Provider<HarvestDefinitionDAO> getHarvestDefinitionDAOProvider() {
        return () -> HarvestDefinitionDAO.getInstance();
    }

    public static Provider<JobDAO> getJobDAOProvider() {
        return () -> JobDAO.getInstance();
    }

    public static Provider<DomainDAO> getDomainDAOProvider() {
        return () -> DomainDAO.getInstance();
    }

    public static Provider<ExtendedFieldDAO> getExtendedFieldDAOProvider() {
        return () -> ExtendedFieldDAO.getInstance();
    }
}
