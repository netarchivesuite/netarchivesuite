package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

public class IndexRequestServerFactory extends SettingsFactory<IndexRequestServerInterface> {

        /**
         * Returns an instance of the chosen IndexRequestServerInterface 
         * implementation defined by the setting
         * settings.archive.indexserver.indexrequestserver.class .
         * This class must have a getInstance method
         * @throws ArgumentNotValid if the instance cannot be constructed.
         * @return an IndexRequestServerInterface instance. 
         */
        public static IndexRequestServerInterface getInstance() throws ArgumentNotValid {
            return SettingsFactory.getInstance(
                    HarvesterSettings.INDEXREQUEST_SERVER_CLASS);
        }
}
