package dk.netarkivet.harvester.scheduler.jobgen;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;


/**
 * Factory class for instantiating a specific implementation
 * of {@link JobGenerator}. The implementation class is defined
 * by the setting {@link HarvesterSettings#JOBGEN_CLASS}
 * &nbsp;(<em>settings.harvester.scheduler.jobgen.class</em>).
 */
public class JobGeneratorFactory extends SettingsFactory<JobGenerator> {

    /**
     * Returns an instance of the configured {@link JobGenerator}
     * implementation defined by the setting {@link HarvesterSettings#JOBGEN_CLASS}.
     * This class must have a constructor or factory method with a
     * signature matching the array args.
     * @param args the arguments to the constructor or factory method
     * @return the {@link JobGenerator} instance.
     */
    public static JobGenerator getInstance(Object ...args)
    throws ArgumentNotValid {
        return SettingsFactory.getInstance(
                HarvesterSettings.JOBGEN_CLASS, args);
    }

}
