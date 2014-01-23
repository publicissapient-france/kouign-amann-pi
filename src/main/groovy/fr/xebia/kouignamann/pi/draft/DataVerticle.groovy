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

    Logger log

    static Boolean READ_ONLY = false

    static File envHome = new File("/tmp")

    private Environment devEnv

    private EntityStore store

    def voterIdx

    def voteIdx


    def start() {

        log = container.logger

        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.findNameByNfcId", this.&findNameByNfcId)
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.store", this.&store)

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
        Voter voter = new Voter()
        voter.name = "Pablo Lopez"
        voter.nfcId = "1D A8 7E ED"
        voterIdx.put(voter)

        voter = new Voter()
        voter.name = "Merle Moqueur"
        voter.nfcId = "00 00 00 00"
        voterIdx.put(voter)

        log.info "START: Done initializing DB"
    }

    def store(Message message) {
        // stores a vote
        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.storeVote ${message}")

        Vote vote = new Vote()
        vote.nfcId = message.body.nfcId
        vote.voteTime = message.body.voteTime
        vote.note = message.body.note

        voteIdx.put(vote)

        log.info("Process -> Storing '${vote}'")

        message.reply([status: 'OK'])
    }

    def delete(Message message) {

    }

    def findNameByNfcId(Message message) {
        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.findNameByNfcId ${message}")
        log.info("Process -> Retrieving '${message.body.nfcId}'")

        def voter = voterIdx.get(message.body.nfcId)

        message.reply([name: (voter ? voter.name : 'Anonyme')])
    }

    /**
     * Should be launched by timer
     */
    def backupAndFlush() {

    }

    def processStoredVotes(Message message) {
        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processStoredVotes ${message}")
        def cursor = voteIdx.entities()
        for (Vote vote : cursor) {
            Map outgoingMessage = [
                    "nfcId": vote.nfcId,
                    "voteTime": vote.voteTime,
                    "note": vote.note,
                    "hardwareUid": container.config.hardwareUid
            ]
            // End point must exists ?
            // TODO Need further testing
            // Send to processor
            log.info("Bus -> ${message.body.nextProcessor} ${message}")
            def eventBus = vertx.eventBus
            def wrapperBus = new WrapperEventBus(eventBus.javaEventBus())
            wrapperBus.sendWithTimeout(message.body.nextProcessor, outgoingMessage, 1000) { result ->
                if (result.succeeded) {
                    log.info("Process -> ${outgoingMessage} successfully processed by central")
                    deleteVoteFromLocal(vote.voteUid)
                } else {
                    log.info("Process -> TIMEOUT from central - Do nothing")
                }

            }
        }
        cursor.close()

    }

    def deleteVoteFromLocal(long voteUid) {
        log.info "Process -> Remove from local base vote ${voteUid}"
        voteIdx.delete(voteUid)
    }

    def stop() {
        if (store) {
            try {
                store.close();
            } catch (DatabaseException dbe) {
                log.error "Stop -> Error closing store: ${dbe.toString()}"
            }
        }

        if (devEnv) {
            try {
                // Finally, close environment.
                devEnv.close();
            } catch (DatabaseException dbe) {
                log.error "Stop -> Error closing MyDbEnv: ${dbe.toString()} "
            }
        }
    }
}
