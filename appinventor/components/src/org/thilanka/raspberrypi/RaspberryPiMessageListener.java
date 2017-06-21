package org.thilanka.raspberrypi;

import java.util.List;

/**
 * Callback interface for receiving messages through the MQTT protocol.
 *
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
public interface RaspberryPiMessageListener {

  /**
   * This method is called when a message is received from a subscribed topic.
   * 
   * @param pTopic
   *          The name topic of the received message.
   * @param pMessage
   *          The contents of the message.
   */
  void MqttMessageReceived(String pTopic, String pMessage);

  /**
   * This method is called when the client has successfully sent the message.
   * 
   * @param pTopic
   *          The name topic of the sent message.
   * @param pMessage
   *          The contents of the message.
   */
  void MqttMessageSent(List<String> pTopic, String pMessage);

  /**
   * This method is called when the connection to the MQTT connection is lost.
   * 
   * @param pError
   *          Error to send back.
   */
  void MqttConnectionLost(String pError);

}