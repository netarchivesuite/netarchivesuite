package dk.netarkivet.archive.arcrepository.distribute;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.utils.CleanupIF;

public class NonFunctionalArcRepositoryServer extends ArchiveMessageHandler implements CleanupIF {

    /** The log. */
       private static final Logger log = LoggerFactory.getLogger(NonFunctionalArcRepositoryServer.class);

    /**
     * Creates and adds a ArcRepositoryMessageHandler as listener on the "TheArcrepos"-queue.
     *
     * @param ar the ArcRepository
     */
    public NonFunctionalArcRepositoryServer(ArcRepository ar) {
        ChannelID channel = Channels.getTheRepos();
        log.info("Listening for arc repository messages on channel '{}'", channel);
        JMSConnectionFactory.getInstance().setListener(channel, this);
    }

    @Override public void onMessage(Message msg) {
        try {
            log.warn(this.getClass().getName() + " received a message of type " + ((ObjectMessage) msg).getObject().getClass().getName());
        } catch (JMSException e) {
            log.warn(e.getMessage());
        }
    }

    @Override public void cleanup() {
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), this);

    }
}
