package dk.netarkivet.archive.indexserver;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;

public class IndexRequestServerFactory extends SettingsFactory<IndexRequestServerInterface> {

        /**
         * Returns an instance of the chosen IndexRequestServerInterface 
         * implementation defined by the setting
         * dk.netarkivet.harvester.harvesting.heritrixController.class .
         * This class must have a constructor or factory method with a
         * signature matching the array args.
         * @param args the arguments to the constructor or factory method
         * @throws ArgumentNotValid if the instance cannot be constructed.
         * @return the HeritrixController instance.
         */
        public static IndexRequestServerInterface getInstance() throws ArgumentNotValid {
            return SettingsFactory.getInstance(
                    ArchiveSettings.INDEXREQUEST_SERVER_CLASS);
        }
}
