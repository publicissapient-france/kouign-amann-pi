package fr.xebia.kouignamann.pi.draft

import com.sleepycat.je.DatabaseException
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig
import com.sleepycat.persist.EntityStore
import com.sleepycat.persist.StoreConfig
import fr.xebia.kouignamann.pi.db.Vote
import fr.xebia.kouignamann.pi.db.Voter
import fr.xebia.kouignamann.pi.util.WrapperEventBus
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

/**
 * Should be deployed as a WorkerVerticle
 */
class DataVerticle extends Verticle {

    static File envHome = new File("/tmp")

    static Boolean READ_ONLY = false

    Logger log

    Environment devEnv

    EntityStore store

    Long backupTimerId

    def voterIdx

    def voteIdx

    def start() {

        log = container.logger

        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.data.findNameByNfcId", this.&findNameByNfcId)
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.data.store", this.&store)

        backupTimerId = vertx.setPeriodic(30000, this.&backupAndFlush)

        try {
            initDb()
        } catch (DatabaseException dbe) {
            log.error "START: Error opening environment and store: ${dbe.toString()}"
        }
    }

    private void initDb() {
        log.info "START: Initializing DB"

        EnvironmentConfig myEnvConfig = new EnvironmentConfig()
        StoreConfig storeConfig = new StoreConfig()

        myEnvConfig.setAllowCreate(!READ_ONLY)
        storeConfig.setAllowCreate(!READ_ONLY)

        // Open the environment and entity store
        devEnv = new Environment(envHome, myEnvConfig)
        store = new EntityStore(devEnv, "EntityStore", storeConfig)

        // Create the index
        voterIdx = store.getPrimaryIndex(String.class, Voter.class);
        voteIdx = store.getPrimaryIndex(Long.class, Vote.class);

        // Load Test data
        voterIdx.put(new Voter(name: 'Pablo Lopez', nfcId: '1D A8 7E ED'))
        voterIdx.put(new Voter(name: 'Merle Moqueur', nfcId: '00 00 00 00'))

        log.info "START: Done initializing DB"
    }

    /**
     * EventBus Handler
     *
     * @param message
     */
    def store(Message message) {
        // stores a vote
        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.storeVote ${message}")

        Vote vote = new Vote()
        vote.nfcId = message.body.nfcId
        vote.voteTime = message.body.voteTime
        vote.note = message.body.note

        voteIdx.put(vote)

        log.info(">> Stored '${vote}'")

        message.reply([status: 'OK'])
    }

    /**
     * EventBus Handler
     *
     * @param message
     */
    def findNameByNfcId(Message message) {
        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.findNameByNfcId ${message}")
        log.info("Process -> Retrieving '${message.body.nfcId}'")

        def voter = voterIdx.get(message.body.nfcId)

        message.reply([name: (voter ? voter.name : 'Anonyme')])
    }

    /**
     * Vertx Periodic
     */
    def backupAndFlush(Long timerId) {

        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processStoredVotes ${message}")

        def cursor = voteIdx.entities()
        for (Vote vote : cursor) {

            Map outgoingMessage = [
                    nfcId: vote.nfcId,
                    voteTime: vote.voteTime,
                    note: vote.note,
                    hardwareUid: container.config.hardwareUid
            ]

            // End point must exists ?
            // TODO Need further testing
            // Send to processor

            log.info("Bus -> ${message.body.nextProcessor} ${message}")

            def eventBus = vertx.eventBus
            def wrapperBus = new WrapperEventBus(eventBus.javaEventBus())

            wrapperBus.sendWithTimeout(message.body.nextProcessor, outgoingMessage, 1000) { result ->
                if (result.succeeded) {
                    log.info(">> ${outgoingMessage} successfully processed by central")
                    delete(vote.voteUid)
                } else {
                    log.info(">> TIMEOUT from central - Do nothing")
                }

            }
        }
        cursor.close()

    }

    private def delete(Long voteUid) {

        log.info(">> Remove from local base vote ${voteUid}")

        voteIdx.delete(voteUid)
    }

    def stop() {
        if (store) {
            try {
                store.close();
            } catch (DatabaseException dbe) {
                log.error "STOP: Error closing store: ${dbe.toString()}"
            }
        }

        if (devEnv) {
            try {
                // Finally, close environment.
                devEnv.close();
            } catch (DatabaseException dbe) {
                log.error "STOP: Error closing MyDbEnv: ${dbe.toString()} "
            }
        }
    }
}
