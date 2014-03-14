package fr.xebia.kouignamann.pi

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json
import org.vertx.java.core.logging.Logger

class MqttVerticle extends Verticle implements MqttCallback {

    private Logger log

    private MqttClient client

    private MqttConnectOptions options

    def start() {

        log = container.logger

        log.info(this.container.config['mqttClient'])

        configure(this.container.config['mqttClient'] as Map)

        log.info('Start -> Initialize handler')

        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote": this.&processVote,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        log.info('Start -> Done initialize handler')
    }

    def stop() {
        log.info('Stop method not implemented yet.')
    }

    def configure(Map config) throws MqttException {
        String uri = config['server-uri']
        String clientId = config['client-id']

        String persistenceDir = config['persistence-dir'] ?: System.getProperty('java.io.tmpdir')
        def persistence = new MqttDefaultFilePersistence(persistenceDir)
        client = new MqttClient(uri, clientId, persistence)

        client.setCallback(this)

        options = new MqttConnectOptions()
        if (config.password) {
            options.setPassword(config.password as char[])
            options.setUserName(config.user)
        }
        options.setCleanSession(true)

        client.connect(options)
        client.disconnect()
    }

    def processVote(Message incomingMsg) {
        log.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote ${incomingMsg}")
        Map outgoingMessage = [
                "nfcId": incomingMsg.body.nfcId,
                "voteTime": incomingMsg.body.voteTime,
                "note": incomingMsg.body.note,
                "hardwareUid": container.config.hardwareUid
        ]

        def message = new MqttMessage(Json.encode(outgoingMessage).getBytes())
        message.setQos(2)

        log.info("Connect")
        if (!client.isConnected()) {
            client?.connect(options)
        }

        log.info("Publish")
        client.publish('fr.xebia.kouignamann.nuc.central.processSingleVote', message)
        log.info("Disconnect")
        client.disconnect()
    }

    @Override
    void connectionLost(Throwable throwable) {
        log.info('connectionLost')
    }

    @Override
    void messageArrived(String s, MqttMessage mqttMessage) {
        log.info('messageArrived')
    }

    @Override
    void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.info('deliveryComplete')
    }
}