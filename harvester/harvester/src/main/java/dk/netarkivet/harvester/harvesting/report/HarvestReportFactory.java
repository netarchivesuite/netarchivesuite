package dk.netarkivet.harvester.harvesting.report;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Factory class for instantiating a specific implementation
 * of {@link HarvestReport}. The implementation class is defined
 * by the setting {@link HarvesterSettings#HARVEST_REPORT_CLASS}
 */
public class HarvestReportFactory extends SettingsFactory<HarvestReport> {

    /**
     * Returns an instance of the default {@link HarvestReport}
     * implementation defined by the setting
     * {@link HarvesterSettings#HARVEST_REPORT_CLASS}.
     * This class must have a constructor or factory method with a
     * signature matching the array args.
     * @param args the arguments to the constructor or factory method
     * @throws ArgumentNotValid if the instance cannot be constructed.
     * @return the {@link HarvestReport} instance.
     */
    public static HarvestReport generateHarvestReport(Object ...args)
    throws ArgumentNotValid, IOFailure {
        return SettingsFactory.getInstance(
                HarvesterSettings.HARVEST_REPORT_CLASS, args);
    }

}
