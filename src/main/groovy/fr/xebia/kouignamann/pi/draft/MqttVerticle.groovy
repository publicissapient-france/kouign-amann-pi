package fr.xebia.kouignamann.pi.draft

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json

class MqttVerticle extends Verticle implements MqttCallback {
    def logger

    MqttAsyncClient client
    MqttConnectOptions options

    /*Object waiter = new Object();
    boolean donext = false;
    Throwable ex = null;


    public int state = BEGIN;

    static final int BEGIN = 0;
    public static final int CONNECTED = 1;
    static final int PUBLISHED = 2;
    static final int SUBSCRIBED = 3;
    static final int DISCONNECTED = 4;
    static final int FINISH = 5;
    static final int ERROR = 6;
    static final int DISCONNECT = 7;
    */

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
        client = new MqttAsyncClient(uri, clientId, persistence)

        client.setCallback(this)

        options = new MqttConnectOptions()
        if (config.password) {
            options.setPassword(config.password as char[])
            options.setUserName(config.user)
        }
        options.setCleanSession(true)

        //options.setKeepAliveInterval(30)
        //options.setConnectionTimeout(0)

        client.connect(options)

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

        def message = new MqttMessage(Json.encode(outgoingMessage).getBytes())
        message.setQos(2)

        if (!client.isConnected()) {
            client?.connect(options)
        }

        def topic = client.getTopic('fr.xebia.kouignamann.nuc.central.processSingleVote')
        //def token = topic.publish(message)
        client.publish('fr.xebia.kouignamann.nuc.central.processSingleVote',message)
        // token.waitForCompletion()
        //client.publish('fr.xebia.kouignamann.nuc.central.processSingleVote', message)

        //if (client) {
        // client.disconnect()
        //}
    }

    @Override
    void connectionLost(Throwable throwable) {
        logger.info "connectionLost"
        while (!client.isConnected()) {
            try {
                client?.connect(options)
                sleep 1000
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }

    @Override
    void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        logger.info "messageArrived"
    }

    @Override
    void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.info "deliveryComplete"
    }

    /*public void publish(String topicName, int qos, byte[] payload) throws Throwable {
        client.connect(options, "Connect sample context", conListener)
        client.publish(topicName, message, "Pub sample context", pubListener)
        client.disconnect("Disconnect sample context", discListener)
    }*/

    /*
    IMqttActionListener conListener = new IMqttActionListener() {
                public void onSuccess(IMqttToken asyncActionToken) {
                    try{
                    //logger.info ("Connected");
                    println "Connected"
                    state = CONNECTED;
                    carryOn();
                    } catch (Exception e){
                        e.printStackTrace()
                    }
                }

                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    ex = exception;
                    state = ERROR;
                    logger.error ("connect failed" + exception);
                    carryOn();
                }

                public void carryOn() {
                    synchronized (waiter) {
                        donext = true;
                        waiter.notifyAll();
                    }
                }
            };
     */

}