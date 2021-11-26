package dk.netarkivet.common.distribute.bitrepository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.ChecksumUtils;
import org.bitrepository.common.utils.SettingsUtils;
import org.bitrepository.modify.ModifyComponentFactory;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.bitrepository.protocol.messagebus.MessageBus;
import org.bitrepository.protocol.messagebus.MessageBusManager;
import org.bitrepository.protocol.security.BasicMessageAuthenticator;
import org.bitrepository.protocol.security.BasicMessageSigner;
import org.bitrepository.protocol.security.BasicOperationAuthorizor;
import org.bitrepository.protocol.security.BasicSecurityManager;
import org.bitrepository.protocol.security.MessageAuthenticator;
import org.bitrepository.protocol.security.MessageSigner;
import org.bitrepository.protocol.security.OperationAuthorizor;
import org.bitrepository.protocol.security.PermissionStore;
import org.bitrepository.protocol.security.SecurityManager;
import org.bitrepository.settings.referencesettings.FileExchangeSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Utility class to abstract away the specifics of setting up and obtaining bitrepository.org clients.  
 */
public class BitmagUtils {
    public static final String BITREPOSITORY_GETFILEIDS_MAX_RESULTS = "settings.common.arcrepositoryClient.bitrepository.getFileIDsMaxResults";
    public static final String BITREPOSITORY_TEMPDIR = "settings.common.arcrepositoryClient.bitrepository.tempdir";
    // optional so we don't force the user to use credentials.
    public static final String BITREPOSITORY_KEYFILENAME = "settings.common.arcrepositoryClient.bitrepository.keyfilename";
    public static final String BITREPOSITORY_STORE_MAX_PILLAR_FAILURES = "settings.common.arcrepositoryClient.bitrepository.storeMaxPillarFailures";
    public static final String BITREPOSITORY_COLLECTIONID = "settings.common.arcrepositoryClient.bitrepository.collectionID";
    public static final String BITREPOSITORY_USEPILLAR = "settings.common.arcrepositoryClient.bitrepository.usepillar";
    public static String BITREPOSITORY_SETTINGS_DIR = "settings.common.arcrepositoryClient.bitrepository.settingsDir";

    private static Settings settings;
    private static SecurityManager securityManager;
    private static Path certificate;

    private static Logger logger = LoggerFactory.getLogger(BitmagUtils.class);

    /**
     * Method to initialize the utility class. Must be called prior to use of any other method 
     * as it initializes internal state.
     */
    public static void initialize() {
        String bitrepositoryDir = dk.netarkivet.common.utils.Settings.get(BITREPOSITORY_SETTINGS_DIR);
        logger.debug("Bitrepository settings will be loaded from {}.", bitrepositoryDir);
        Path configurationDir = Paths.get(bitrepositoryDir);
        logger.debug("Path to Bitrepository settings is {}.", configurationDir);
        String clientCertificateName = dk.netarkivet.common.utils.Settings.get(BITREPOSITORY_KEYFILENAME);
        if (clientCertificateName.isEmpty()) {
            certificate = configurationDir.resolve(UUID.randomUUID().toString()); // Random for no certificate
        } else {
            certificate = configurationDir.resolve(clientCertificateName);
        }
        settings = loadSettings(configurationDir);
        securityManager = loadSecurityManager(); 
    }
    
    /**
     * Load settings based on configuration found in configurationDir.
     * Use CommandLineSettingsProvider as that will help give a convenient clientID. 
     */
    private static Settings loadSettings(Path configurationDir) {
        SettingsProvider settingsProvider =
                new SettingsProvider(new XMLFileSettingsLoader(configurationDir.toString()), generateComponentID());
        Settings settings = settingsProvider.getSettings();
        SettingsUtils.initialize(settings);
        return settings;
    }
    
    /**
     * Boiler plate for loading permission model.  
     */
    private static SecurityManager loadSecurityManager() {
        ArgumentValidator.checkNotNull(settings, "Settings settings");
        PermissionStore permissionStore = new PermissionStore();
        MessageAuthenticator authenticator = new BasicMessageAuthenticator(permissionStore);
        MessageSigner signer = new BasicMessageSigner();
        OperationAuthorizor authorizer = new BasicOperationAuthorizor(permissionStore);
        return new BasicSecurityManager(settings.getRepositorySettings(), certificate.toString(),
                authenticator, signer, authorizer, permissionStore,
                settings.getComponentID());
    }

    /**
     * Method to get the list of known pillars in a collection
     * @param collectionID The ID of the collection to obtain pillars for
     * @return the list of known pillarIDs 
     */
    public static List<String> getKnownPillars(String collectionID) {
        return SettingsUtils.getPillarIDsForCollection(collectionID);
    }
    
    /**
     * Method to get the list of known collections
     * @return the list of known collectionIDs 
     */
    public static List<String> getKnownCollections() {
        return SettingsUtils.getAllCollectionsIDs();
    }

    /**
     * Generates a component id, which includes the hostname and a random UUID.
     * @return The Bitrepository component id for this NetarchiveSuite application.
     */
    public static String generateComponentID() {
        String hn = HostNameUtils.getHostName();
        return "NetarchivesuiteClient-" + hn + "-" + UUID.randomUUID();
    }

    /**
     * Method to get the base part of the URL to the file exchange server. 
     * @return {@link URL} URL with the base part of the file exchange server.  
     * @throws MalformedURLException if the file exchange configuration results in an invalid url
     */
    public static URL getFileExchangeBaseURL() throws MalformedURLException {
        FileExchangeSettings feSettings = settings.getReferenceSettings().getFileExchangeSettings();
        return new URL(feSettings.getProtocolType().value(), feSettings.getServerName(), 
                feSettings.getPort().intValue(), feSettings.getPath() + "/");
    }
    
    /**
     * Method to retrieve the instance of the FileExchange. 
     * To be used for transferring files to and from the FileExchange server
     * @return {@link FileExchange} The FileExchange instance
     */
    public static FileExchange getFileExchange() {
        return ProtocolComponentFactory.getInstance().getFileExchange(settings);
    }

    /**
     * Creates the data structure for encapsulating the validation checksums for validation of the PutFile operation.
     * @param file The file to have the checksum calculated.
     * @param csSpec A given ChecksumSpecTYPE
     * @return The ChecksumDataForFileTYPE for the pillars to validate the PutFile operation.
     */
    public static ChecksumDataForFileTYPE getValidationChecksum(File file, ChecksumSpecTYPE csSpec) {
        ArgumentNotValid.checkExistsNormalFile(file, "File file");
        ArgumentNotValid.checkNotNull(csSpec, "ChecksumSpecTYPE csSpec");
        String checksum = ChecksumUtils.generateChecksum(file, csSpec);
        ChecksumDataForFileTYPE res = new ChecksumDataForFileTYPE();
        res.setCalculationTimestamp(CalendarUtils.getNow());
        res.setChecksumSpec(csSpec);
        res.setChecksumValue(Base16Utils.encodeBase16(checksum));
        return res;
    }
    
    public static ChecksumSpecTYPE getChecksumSpec(ChecksumType checksumType) {
        ChecksumSpecTYPE checksumSpec = new ChecksumSpecTYPE();
        checksumSpec.setChecksumType(checksumType);
        return checksumSpec;
    }
    
    /**
     * Retreive a PutFileClient
     * @return {@link PutFileClient} The PutFileClient
     */
    public static PutFileClient getPutFileClient() {
        return ModifyComponentFactory.getInstance().retrievePutClient(settings, 
                securityManager, settings.getComponentID());
    }
    
    /**
     * Retreive a GetFileIDsClient
     * @return {@link GetFileIDsClient} The GetFileIDsClient
     */
    public static GetFileIDsClient getFileIDsClient() {
        return AccessComponentFactory.getInstance().createGetFileIDsClient(settings, securityManager,
                settings.getComponentID());
    }
    
    /**
     * Retreive a GetChecksumsClient
     * @return {@link GetChecksumsClient} The GetChecksumsClient
     */
    public static GetChecksumsClient getChecksumsClient() {
        return AccessComponentFactory.getInstance().createGetChecksumsClient(settings, securityManager,
                settings.getComponentID());
    }
    
    /**
     * Retreive a GetFileClient
     * @return {@link GetFileClient} The GetFileClient
     */
    public static GetFileClient getFileClient() {
        return AccessComponentFactory.getInstance().createGetFileClient(settings, securityManager, 
                settings.getComponentID());
    }

    /**
     * Retrieves the default collectionID to use from settings.
     * If the collectionID is not set it defaults to the environment name.
     * @return The ID of the default collection to execute actions on.
     */
    public static String getDefaultCollectionID() {
        String collectionId = dk.netarkivet.common.utils.Settings.get(BitmagUtils.BITREPOSITORY_COLLECTIONID);
        if (collectionId == null || collectionId.trim().isEmpty()) {
            collectionId = dk.netarkivet.common.utils.Settings.get(CommonSettings.ENVIRONMENT_NAME);
        }
        return collectionId;
    }
    
    /**
     * Method to shutdown the bitrepository connections if such exists. 
     * Should only be called when all interactions with the bitrepository from this client is done.
     * @throws JMSException if there is trouble shutting down the messagebus connection
     */
    public static void shutdown() throws JMSException {
        MessageBus messageBus = MessageBusManager.getMessageBus();
        if (messageBus != null) {
            messageBus.close();
        }
    }
}
