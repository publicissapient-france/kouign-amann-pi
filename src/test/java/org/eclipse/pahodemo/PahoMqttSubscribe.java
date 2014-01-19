package org.eclipse.pahodemo;

import org.eclipse.paho.client.mqttv3.*;

public class PahoMqttSubscribe implements MqttCallback {

    MqttClient client;

    public static void main(String[] args) {
        new PahoMqttSubscribe().doDemo();
    }

    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        System.out.println(topic + " " + new String(message.getPayload()));
    }

    public void connectionLost(Throwable cause) {
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public void doDemo() {
        try {
            client = new MqttClient("tcp://m10.cloudmqtt.com:10325",
                    MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("kouign-amann");
            options.setPassword("kouign-amann".toCharArray());
            client.connect(options);
            client.setCallback(this);
            client.subscribe("fr.xebia.kouignamann.nuc.central.processSingleVote");
            // We’ll now idle here sleeping, but your app can be busy
            // working here instead
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}