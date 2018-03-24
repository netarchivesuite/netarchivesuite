/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.indexserver.distribute;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.FTPRemoteFile;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileSettings;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.indexserver.MultiFileBasedCache;

/**
 * Client for index request server.
 * <p>
 * Allows to request an index of some type over a list of jobs. Factory method will return the index request client of
 * the type wished.
 */
public class IndexRequestClient extends MultiFileBasedCache<Long> implements JobIndexCache {

    /** Logger for this indexRequestClient. */
    private static final Logger log = LoggerFactory.getLogger(IndexRequestClient.class);

    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath = "dk/netarkivet/harvester/"
            + "indexserver/distribute/IndexRequestClientSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
    }

    /** Synchronizer used to make requests. */
    private static Synchronizer synchronizer;

    /** Factory method map of clients created of specific types. */
    private static Map<RequestType, IndexRequestClient> clients = new EnumMap<RequestType, IndexRequestClient>(
            RequestType.class);

    /** The type of this indexRequestClient. */
    private RequestType requestType;

    /**
     * <b>settings.common.indexClient.indexRequestTimeout</b>: <br>
     * Setting for the amount of time, in milliseconds, we should wait for replies when issuing a call to generate an
     * index over some jobs.
     */
    public static final String INDEXREQUEST_TIMEOUT = "settings.common.indexClient.indexRequestTimeout";

    /**
     * <b>settings.common.indexClient.useLocalFtpServer</b>: <br>
     * Setting for using the ftpserver assigned to the client instead of the one assigned to the indexserver. Set to
     * false by default.
     */
    public static final String INDEXREQUEST_USE_LOCAL_FTPSERVER = "settings.common.indexClient.useLocalFtpServer";

    /**
     * Initialise this client, handling requests of a given type. Start listening to channel if not done yet.
     *
     * @param type Type of this cache
     */
    private IndexRequestClient(RequestType type) {
        super(type.name());
        this.requestType = type;
    }

    /**
     * Get the singleton synchronizer for sending requests.
     *
     * @return synchronizer
     */
    private synchronized Synchronizer getSynchronizer() {
        if (synchronizer == null) {
            synchronizer = new Synchronizer();
            JMSConnectionFactory.getInstance().setListener(Channels.getThisIndexClient(), synchronizer);
        }
        return synchronizer;
    }

    /**
     * Factory method returning an IndexRequestClient for the given type of index cache.
     *
     * @param type The type of this cache.
     * @return The singleton instance dedicated to this type of index requests.
     * @throws ArgumentNotValid if type is null.
     */
    public static synchronized IndexRequestClient getInstance(RequestType type) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(type, "RequestType type");
        IndexRequestClient client = clients.get(type);
        if (client == null) {
            client = new IndexRequestClient(type);
            clients.put(type, client);
        }
        return client;
    }

    /**
     * This method makes sure the actual caching of underlying data is done using the index server. It will convert
     * calls into an IndexRequestMessage which is sent to the server. The Set&lt;Long&gt; of found jobs, and the side
     * effect of caching the index, is done using this communication with the server. The resulting files will be
     * unzipped into the cache dir.
     * <p>
     * This method should not be called directly! Instead call cache() or getIndex().
     *
     * @param jobSet The set of job IDs.
     * @return The set of found job IDs.
     * @throws ArgumentNotValid on null argument; or on wrong parameters in replied message.
     * @throws IOFailure on trouble in communication or invalid reply types.
     * @throws IllegalState if message is not OK.
     * @see dk.netarkivet.harvester.indexserver.FileBasedCache#cache
     * @see dk.netarkivet.harvester.indexserver.FileBasedCache#getIndex
     */
    protected Set<Long> cacheData(Set<Long> jobSet) throws IOFailure, IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(jobSet, "Set<Long> id");

        if (log.isInfoEnabled()) {
            log.info("Requesting an index of type '{}' for the jobs [{}]", this.requestType,
                    StringUtils.conjoin(",", jobSet));
        }
        // use locally defined ftp-server, if required
        RemoteFileSettings ftpSettings = null;

        if (useLocalFtpserver()) {
            log.debug("Requesting the use of the FTPserver defined locally.");
            ftpSettings = FTPRemoteFile.getRemoteFileSettings();
        }

        // Send request to server
        IndexRequestMessage irMsg = new IndexRequestMessage(requestType, jobSet, ftpSettings);
        if (log.isDebugEnabled()) {
            log.debug("Waiting {} for the index", TimeUtils.readableTimeInterval(getIndexTimeout()));
        }
        NetarkivetMessage msg = getSynchronizer().sendAndWaitForOneReply(irMsg, getIndexTimeout());

        checkMessageValid(jobSet, msg);
        IndexRequestMessage reply = (IndexRequestMessage) msg;

        Set<Long> foundJobs = reply.getFoundJobs();
        // Only if all jobs asked for were found will the result contain files.
        Set<Long> diffSet = new HashSet<Long>(jobSet);
        diffSet.removeAll(foundJobs);
        if (diffSet.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Successfully received an index of type '{}' for the jobs [{}]", this.requestType,
                        StringUtils.conjoin(",", jobSet));
            }
            try {
                if (reply.isIndexIsStoredInDirectory()) {
                    gunzipToDir(reply.getResultFiles(), getCacheFile(jobSet));
                } else {
                    unzipAndDeleteRemoteFile(reply.getResultFile(), getCacheFile(jobSet));
                }
            } catch (IOFailure e) {
                log.warn("IOFailure during unzipping of index", e);
                return new HashSet<Long>();
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No index received. The following jobs were not found: {}", StringUtils.conjoin(",", diffSet));
            }
        }

        // Return the set of found jobs
        return foundJobs;
    }

    /**
     * Gunzip a list of RemoteFiles into a given directory. The actual unzipping takes place in a temporary directory
     * which gets renamed, so the directory appears to be created atomically.
     *
     * @param files List of RemoteFiles to gunzip. The RemoteFiles will be deleted as part of the process.
     * @param toDir The directory that the gunzipped files will eventually be placed in. This directory will be created
     * and filled atomically.
     * @throws IOFailure If errors occur during unzipping, e.g. disk full.
     */
    private void gunzipToDir(List<RemoteFile> files, File toDir) throws IOFailure {
        File tmpDir = FileUtils.createUniqueTempDir(toDir.getParentFile(), toDir.getName());
        try {
            FileUtils.createDir(tmpDir);
            for (RemoteFile f : files) {
                String destFileName = f.getName();
                destFileName = destFileName.substring(0, destFileName.length() - ZipUtils.GZIP_SUFFIX.length());
                File destFile = new File(tmpDir, destFileName);
                unzipAndDeleteRemoteFile(f, destFile);
            }
            if (!tmpDir.renameTo(toDir)) {
                throw new IOFailure("Error renaming temp dir '" + tmpDir + "' to target directory '"
                        + toDir.getAbsolutePath() + "'");
            }
        } finally {
            FileUtils.removeRecursively(tmpDir);
        }
    }

    /**
     * Unzip a RemoteFile to a given file, deleting the RemoteFile afterwards. Problems arising while deleting are
     * logged, but do not cause exceptions.
     *
     * @param remoteFile A file to download. This file will be attempted deleted after successfull unzipping.
     * @param destFile A place to put the unzipped file.
     * @throws IOFailure on any I/O error, e.g. disk full
     */
    private void unzipAndDeleteRemoteFile(RemoteFile remoteFile, File destFile) throws IOFailure {
        File tmpFile = null;
        try {
            // We cannot unzip directly from a stream, so we make a temp file.
            tmpFile = File.createTempFile("remotefile-unzip", ".gz", FileUtils.getTempDir());
            remoteFile.copyTo(tmpFile);
            ZipUtils.gunzipFile(tmpFile, destFile);
            try {
                remoteFile.cleanup();
            } catch (IOFailure e) {
                log.debug("Trouble deleting file '" + remoteFile.getName() + "' from FTP server after saving it", e);
            }
        } catch (IOException e) {
            // All other IOExceptions have already been turned into IOFailure
            throw new IOFailure("Error making temporary file in " + FileUtils.getTempDir(), e);
        } finally {
            if (tmpFile != null) {
                FileUtils.remove(tmpFile);
            }
        }
    }

    /**
     * How long should we wait for index replies?
     *
     * @return Index timeout value in milliseconds.
     */
    protected long getIndexTimeout() {
        // TODO It might be a good idea to make this dependant on "type"
        return Settings.getLong(INDEXREQUEST_TIMEOUT);
    }

    /**
     * Check if we should use local ftpserver or not, provided you are using FTPRemoteFile as the
     * {@link CommonSettings#REMOTE_FILE_CLASS}. This always returns false, when
     * {@link CommonSettings#REMOTE_FILE_CLASS} is not {@link FTPRemoteFile}.
     *
     * @return true, if we should use the local ftpserver when retrieving data from the indexserver, false, if the
     * indexserver should decide for us.
     */
    protected boolean useLocalFtpserver() {
        // check first that RemoteFileClass is FTPRemoteFile
        String remotefileClassname = Settings.get(CommonSettings.REMOTE_FILE_CLASS);
        if (!remotefileClassname.equalsIgnoreCase(FTPRemoteFile.class.getName())) {
            log.debug("Not using localftpserver as transport, because this application uses " + remotefileClassname
                    + " as file transport class");
            return false;
        } else {
            return Settings.getBoolean(INDEXREQUEST_USE_LOCAL_FTPSERVER);
        }
    }

    /**
     * Check the reply message is valid.
     *
     * @param jobSet The requested set of jobs
     * @param msg The message received
     * @throws ArgumentNotValid On wrong parameters in replied message.
     * @throws IOFailure on trouble in communication or invalid reply types.
     * @throws IllegalState if message is not OK.
     */
    private void checkMessageValid(Set<Long> jobSet, NetarkivetMessage msg) throws IllegalState, IOFailure,
            ArgumentNotValid {
        // Read and check reply
        if (msg == null) {
            throw new IOFailure("Timeout waiting for reply of index request for jobs "
                    + StringUtils.conjoin(",", jobSet));
        }
        if (!msg.isOk()) {
            throw new IllegalState("Reply message not ok. Message is: '" + msg.getErrMsg()
                    + "' in index request for jobs " + StringUtils.conjoin(",", jobSet));
        }
        if (!(msg instanceof IndexRequestMessage)) {
            throw new IOFailure("Unexpected type of reply message: '" + msg.getClass().getName()
                    + "' in index request for jobs " + StringUtils.conjoin(",", jobSet));
        }
        IndexRequestMessage reply = (IndexRequestMessage) msg;
        Set<Long> foundJobs = reply.getFoundJobs();
        if (foundJobs == null) {
            throw new ArgumentNotValid("Missing parameter foundjobs in reply to index request for jobs "
                    + StringUtils.conjoin(",", jobSet));
        }

        // FoundJobs should always be a subset
        if (!jobSet.containsAll(foundJobs)) {
            throw new ArgumentNotValid("foundJobs is not a subset of requested jobs. Requested: "
                    + StringUtils.conjoin(",", jobSet) + ". Found: " + StringUtils.conjoin(",", foundJobs));
        }

        if (jobSet.equals(foundJobs)) {
            // Files should only be present if jobSet=foundJobs
            if (reply.isIndexIsStoredInDirectory()) {
                List<RemoteFile> files;
                files = reply.getResultFiles();
                if (files == null) {
                    throw new ArgumentNotValid("Missing files in reply to" + " index request for jobs "
                            + StringUtils.conjoin(",", jobSet));
                }
            } else {
                RemoteFile file = reply.getResultFile();
                if (file == null) {
                    throw new ArgumentNotValid("Missing file in reply to" + " index request for jobs "
                            + StringUtils.conjoin(",", jobSet));
                }
            }
        }
    }

    /**
     * Method to request an Index without having the result sent right away.
     *
     * @param jobSet The set of job IDs.
     * @param harvestId The ID of the harvest requesting this index.
     * @throws IOFailure On trouble in communication or invalid reply types.
     * @throws IllegalState if message is not OK.
     * @throws ArgumentNotValid if the jobSet is null.
     */
    public void requestIndex(Set<Long> jobSet, Long harvestId) throws IOFailure, IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(jobSet, "Set<Long> id");

        if (log.isInfoEnabled()) {
            log.info("Requesting an index of type '{}' for the jobs [{}]", this.requestType,
                    StringUtils.conjoin(",", jobSet));
        }

        // Send request to server but ask for it not to be returned
        // Ask that a message is sent to the scheduling queue when
        // index is finished

        IndexRequestMessage irMsg = new IndexRequestMessage(requestType, jobSet, HarvesterChannels.getTheSched(), false,
                harvestId);

        JMSConnectionFactory.getInstance().send(irMsg);
    }

}
