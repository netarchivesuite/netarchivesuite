package dk.netarkivet.common.distribute.bitrepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import javax.jms.JMSException;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.client.CommandLineSettingsProvider;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
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

/**
 * Utility class to abstract away the specifics of setting up and obtaining bitrepository.org clients.  
 */
public class BitmagUtils {
    public static final String BITREPOSITORY_GETFILEIDS_MAX_RESULTS = "settings.common.arcrepositoryClient.bitrepository.getFileIDsMaxResults";
    
    private static Settings settings;
    private static SecurityManager securityManager;
    private static Path certificate;
    private static Path configDir;
       
    /**
     * Method to initialize the utility class. Must be called prior to use of any other method 
     * as it initializes internal state.
     * @param configurationDir Path to the configuration base directory, this is the directory where 
     * RepositorySettings.xml and ReferenceSettings.xml is expected to be found. 
     * @param clientCertificate Path to the clients certificate.   
     */
    public static void initialize(Path configurationDir, Path clientCertificate) {
        configDir = configurationDir;
        certificate = clientCertificate;
        settings = loadSettings(configDir);
        securityManager = loadSecurityManager(); 
    }
    
    /**
     * Load settings based on configuration found in configurationDir.
     * Use CommandLineSettingsProvider as that will help give a convenient clientID. 
     */
    private static Settings loadSettings(Path configurationDir) {
        SettingsProvider settingsLoader =
                new CommandLineSettingsProvider(new XMLFileSettingsLoader(configurationDir.toString()));
        Settings settings = settingsLoader.getSettings();
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
     * Method to build the datastructure to transport checksums in
     * @param checksum The string form of the checksum 
     * @return {@link ChecksumDataForFileTYPE} The bitrepository.org data structure 
     * to transport the checksum 
     */
    public static ChecksumDataForFileTYPE getChecksum(String checksum) {
        ChecksumDataForFileTYPE checksumData = new ChecksumDataForFileTYPE();
        checksumData.setChecksumValue(Base16Utils.encodeBase16(checksum));
        checksumData.setCalculationTimestamp(CalendarUtils.getNow());
        ChecksumSpecTYPE checksumSpec = new ChecksumSpecTYPE();
        checksumSpec.setChecksumType(ChecksumType.MD5);
        checksumData.setChecksumSpec(checksumSpec);
        return checksumData;
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
