package fr.xebia.kouignamann.pi.mqtt

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json

class MqttDataManagementVerticle extends Verticle implements MqttCallback {
    def logger

    MqttClient client
    MqttConnectOptions options

    int retryConnectionCounter = 0

    def start() {
        logger = container.logger

        configure(this.container.config['mqttClient'] as Map)

        logger.info "Start -> Initialize handler";
        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote": this.&processVote,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Start -> Done initialize handler";
    }

    def configure(Map config) throws MqttException {
        String uri = config['server-uri']
        String clientId = config['client-id']

        String persistenceDir = config['persistence-dir'] ?: System.getProperty('java.io.tmpdir')
        def persistence = new MqttDefaultFilePersistence(persistenceDir)
        client = new MqttClient(uri, clientId, persistence)

        client.setCallback(this)

        options = new MqttConnectOptions()
        options.setPassword(config.password as char[])
        options.setUserName(config.user)

        client.connect(options)
        if (client.connected) {
            this.retryConnectionCounter = 0
        }
        client.disconnect()
    }

    def processVote(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote ${incomingMsg}")
        Map outgoingMessage = [
                "nfcId": incomingMsg.body.nfcId,
                "voteTime": incomingMsg.body.voteTime,
                "note": incomingMsg.body.note,
                "hardwareUid": container.config.hardwareUid
        ]

        client?.connect(options)

        def topic = client.getTopic('fr.xebia.kouignamann.nuc.central.processSingleVote')
        def message = new MqttMessage(Json.encode(outgoingMessage).getBytes())
        message.setQos(2)
        def token = topic.publish(message)
        // token.waitForCompletion()

        client.disconnect()
    }

    @Override
    void connectionLost(Throwable throwable) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
