package dk.netarkivet.harvester.datamodel.dao;

import javax.inject.Provider;

import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;

public class DAOProviderFactory {
	
    public static Provider<HarvestDefinitionDAO> getHarvestDefinitionDAOProvider() {
        return new Provider<HarvestDefinitionDAO>(){
            @Override
            public HarvestDefinitionDAO get() {
                return HarvestDefinitionDAO.getInstance();
            }
        };
    }

    public static Provider<JobDAO> getJobDAOProvider() {
        return new Provider<JobDAO>(){
            @Override
            public JobDAO get() {
                return JobDAO.getInstance();
            }
        };
    }

    public static Provider<DomainDAO> getDomainDAOProvider() {
        return new Provider<DomainDAO>(){
            @Override
            public DomainDAO get() {
                return DomainDAO.getInstance();
            }
        };
    }

    public static Provider<ExtendedFieldDAO> getExtendedFieldDAOProvider() {
        return new Provider<ExtendedFieldDAO>(){
            @Override
            public ExtendedFieldDAO get() {
                return ExtendedFieldDAO.getInstance();
            }
        };
    }

    public static Provider<EAV> getEAVDAOProvider() {
    	return new Provider<EAV>() {
    		@Override
    		public EAV get() {
    			 return EAV.getInstance();
    		}
    	};
    }

    /*
    public static Provider<HarvestDefinitionDAO> getHarvestDefinitionDAOProvider() {
        return (Provider<HarvestDefinitionDAO>) HarvestDefinitionDAO.getInstance();
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

    public static Provider<EAV> getEAVDAOProvider() {
    	return () -> EAV.getInstance();
    }
    */

}
