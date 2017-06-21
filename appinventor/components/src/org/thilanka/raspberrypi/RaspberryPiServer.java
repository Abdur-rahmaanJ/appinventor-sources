package org.thilanka.raspberrypi;

import java.util.List;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import edu.mit.mqtt.raspberrypi.model.messaging.Action;
import edu.mit.mqtt.raspberrypi.model.messaging.Topic;

/**
 * Non-visible component that models the Raspberry Pi and can act as the MQTT
 * broker that relays messages between the RaspberryPiPinClients and other
 * external sources. The broker, and thus the RaspberryPiServer has an IP
 * address that clients can connect via TCP/IP. For the RaspberryPi component,
 * it is necessary to have the MQTT clients such as sensors and LED outputs to
 * be subscribed to certain topics via the broker. The App Inventor developer
 * can opt to connect to external MQTT brokers or run an MQTT broker on the
 * Raspberry PI device.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(description = "<p>A non-visible component that models the Raspberry Pi and acts as "
    + "an MQTT broker that relays messages between the RaspberryPiPinClients and other "
    + "external MQTT based sources.</p>",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png", 
    version = 1)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "aadroid.permission.INTERNET")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.0.2.jar, org.eclipse.paho.client.mqttv3-1.0.2.jar, " +
    "raspberrypi-mqtt-messages-1.0-SNAPSHOT.jar")
public class RaspberryPiServer extends AndroidNonvisibleComponent implements Component, RaspberryPiMessageListener {

  //RaspberryPi Server Component default values
  private static final String RASPBERRYPI_SERVER_VALUE = "Pi2B"; //Defaults to "Pi2 B" which is the most prevalent model as of June 2016
  private static final String RASPBERRYPI_SERVER_IPV4_VALUE = "iot.eclipse.org";
  private static final int RASPBERRYPI_SERVER_PORT_VALUE = 1883;
  private static final int RASPBERRYPI_SERVER_QOS_VALUE = 2;

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = "RaspberryPiServer";

  private String model;
  private String serverAddress;
  private int port;
  private String identifier;
  private int qos;
  private int pins;
  private boolean shutdown = false;

  private RaspberryPiMessagingService mRaspberryPiMessagingService;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer the container that the component will be placed in
   */
  public RaspberryPiServer(ComponentContainer pContainer) {
    super(pContainer.$form());

    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the RaspberryPiServer Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mRaspberryPiMessagingService = new RaspberryPiMessagingService(context, handler);
    mRaspberryPiMessagingService.addListener(this);

  }

  /**
   * RaspberryPi Model and version. (e.g. A, B, B+, Compute, etc). Depending on
   * the model, there will be constraints on the number of pins, and other
   * features.
   *
   * @param pModel
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = RASPBERRYPI_SERVER_VALUE)
  @SimpleProperty(description = "Sets the model version of the Raspberry Pi. "
      + "Depending on this model input, there will be pin validation in the RaspberryPiPinClient inputs.",
      userVisible = true)
  public void Model(String pModel) {
    model = pModel;
    if (pModel.equals("Pi1A") || pModel.equals("Pi1B")) {
      pins = 26;
    } else if (pModel.equals("Pi1A+") || pModel.equals("Pi1B+") || pModel.equals("Pi2B") || pModel.equals("Pi3B")) {
      pins = 40;
    } else {
      Log.e(LOG_TAG, "Unsupported RasberryPi Model");
    }
  }

  /**
   * Returns the model of the RaspberryPiServer.
   *
   * @return model
   */
  @SimpleProperty(description = "Returns the model type of the RaspberryPi Server",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String Model() {
    return model;
  }

  /**
   * The IP Address or the named address of the Raspberry Pi device. The MQTT
   * broker should be reachable via this address.
   * 
   * @param pServerAddress
   *          the IP address of the Raspberry Pi
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = RASPBERRYPI_SERVER_IPV4_VALUE)
  @SimpleProperty(description = "The IPAddress or the server address of the Raspberry "
      + "Pi device acting as an MQTT broker or any other external MQTT broker "
      + "(such as iot.eclipse.org:1883) used in the communication.",
      userVisible = true)
  public void ServerAddress(String pServerAddress) {
    serverAddress = pServerAddress;
  }

  /**
   * Returns the ipv4 address of the RaspberryPiServer.
   *
   * @return Ipv4Address
   */
  @SimpleProperty(description = "Returns the address of the RaspberryPi Server",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String ServerAddress() {
    return serverAddress;
  }

  /**
   * The TCP/IP port that the MQTT broker on the RaspberryPi is running on.
   * 
   * @param pPort
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = RASPBERRYPI_SERVER_PORT_VALUE + "")
  @SimpleProperty(description = "The TCP/IP port that the MQTT broker on the RaspberryPi is running on.",
      userVisible = true)
  public void Port(int pPort) {
    if (pPort >= 1024 && pPort <= 65535) {
      port = pPort;
    } else {
      throw new ConnectionError("Please enter a valid port number. You entered " + pPort);
    }
  }

  /**
   * Returns the TCP/IP port that the MQTT broker on the RaspberryPi is running
   * on.
   *
   * @return port
   */
  @SimpleProperty(description = "Returns the TCP/IP port that the MQTT broker on the RaspberryPi is running on.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public int Port() {
    return port;
  }

  /**
   * Quality of Service parameter for a RaspberryPi Server broker, which
   * guarantees the received status of the messages.
   * 
   * @param pQos
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = RASPBERRYPI_SERVER_QOS_VALUE + "")
  @SimpleProperty(description = "Quality of Service parameter for a RaspberryPi Server broker, "
      + "which guarantees the received status of the messages. Defaults to 2",
      userVisible = true)
  public void Qos(int pQos) {
    if (pQos > 2) {
      Log.e(LOG_TAG, "QOS was " + pQos + ". It has to be either 0, 1 or 2.");
    }
    qos = pQos;

  }

  /**
   * Returns the Quality of Service parameter for a RaspberryPi Server broker,
   * which guarantees the received status of the messages.
   *
   * @return model
   */
  @SimpleProperty(description = "Returns the Quality of Service parameter for a RaspberryPi Server broker, "
      + "which guarantees the received status of the messages.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String Qos() {
    return model;
  }

  /**
   * Initializes the RaspberryPiServer to send and receive MQTT messages.
   * @param model
   * @param brokerAddress
   * @param brokerPort
   */
  @SimpleFunction(description = "Initializes the RaspberryPiServer component with the given "
      + "RaspberryPi Model, MQTT broker address, port and the identifier given by the companion app "
      + "running on the RaspberryPi device.")
  public void Initialize(String model, String brokerAddress, int brokerPort, String identifier) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Initializing the RaspberryPiServer properties...");
    }
    this.model = model;
    this.serverAddress = brokerAddress;
    this.port = brokerPort;
    this.identifier = identifier;
  }
 
  /**
   * Checks if the given pin can work on the RaspberryPi model being used.
   * 
   * @param pinNumber
   * @return true if this pin is available on the specified RaspberryPi model.
   */
  @SimpleFunction(description = "Returns true if this pin is available on the specified RaspberryPi model.")
  public boolean HasPin(int pinNumber) {
    return pinNumber <= pins && pinNumber > 0;
  }

  /**
   * Shutdown the RaspberryPi GPIO provider.
   */
  @SimpleFunction(description = "Shutdown the RaspberryPi GPIO provider.")
  public void Shutdown() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Shutting down the RaspberryPi Server...");
    }
    shutdown = true;
    // TODO
    mRaspberryPiMessagingService.publish(getInternalTopic(), Action.SHUTDOWN.name());
    if (DEBUG) {
      Log.d(LOG_TAG, "Completed shutting down the RaspberryPi Server.");
    }
  }

  /**
   * Determines the status of the RaspberryPi GPIO Provider.
   * 
   * @return true if the RaspberryPi GPIO provider has been shutdown.
   */
  @SimpleFunction(description = "Returns true if the RaspberryPi GPIO provider has been shutdown.")
  public boolean IsShutdown() {
    return shutdown;
  }

  /**
   * Connected clients are the active devices through the MQTT broker.
   * 
   * @return the number of connected clients.
   */
  @SimpleFunction(description = "Returns the number of connected clients.")
  public int ConnectedClients() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Getting the connected clients...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO pass the message to the RaspberryPiAppInventorCompanion
    return 0;
  }

  /**
   * Disconnected Clients were once connected to the MQTT broker.
   * 
   * @return the number of clients that were registered with the RaspberryPi
   *         broker, but are now disconnected.
   */
  @SimpleFunction(description = "Returns the number of clients that were once "
      + "registered with the RaspberryPi broker, but are now disconnected.")
  public int DisconnectedClients() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Getting the disconnected clients...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO pass the message to the RaspberryPiAppInventorCompanion
    return 0;
  }

  /**
   * Returns true if the client with the given pin number is connected.
   * 
   * @param pin
   * @return true if the client with the given pin number is connected.
   */
  @SimpleFunction(description = "Returns true if the client with the given pin number is connected.")
  public boolean IsClientConnected(int pin) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Checking if the client is connected...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO pass the message to the RaspberryPiAppInventorCompanion
    return false;
  }
 
  /**
   * Indicates that the RaspberryPiServer has shutdown.
   * 
   * @return true when the server has shutdown.
   */
  @SimpleEvent(description = "Indicates that the RaspberryPiServer has shutdown.")
  public boolean HasShutdown() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if the RaspberryPi Server has shutdown.");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO subscribe to a message from the broker via the
    // RaspberryPiAppInventorCompanion
    return false;
  }

  /**
   * Indicates that a certain client attached to the given pin has connected.
   * 
   * @return true when the given client has connected.
   */
  @SimpleEvent(description = "Indicates that a certain client attached to the given pin has connected.")
  public boolean ClientConnected(int pin) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if a client has connected.");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO subscribe to a message from the broker via the
    // RaspberryPiAppInventorCompanion
    return false;
  }

  /**
   * Indicates that a certain client attached to the given pin has disconnected.
   * 
   * @param pin
   * @return true if the given pin has an active client
   */
  @SimpleEvent(description = "Indicates that a certain client attached to the given pin has disconnected.")
  public boolean ClientDisconnected(int pin) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if a client has diconnected.");
      Log.d(LOG_TAG, "To be implemented. ");
    }
    // TODO subscribe to a message from the broker via the
    // RaspberryPiAppInventorCompanion.
    return false;
  }

  /**
   * Indicates that a message is received through MQTT.
   * 
   * @param topic
   * @param message
   */
  @Override
  @SimpleEvent(description = "Indicates that a message is received through MQTT.")
  public void MqttMessageReceived(String topic, String message) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Mqtt Message " + message + " received on subject " + topic + ".");
    }
    if (topic != null && message != null && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MqttMessageReceived", topic, message);
    }
  }

  /**
   * Indicates that a given message belonging to the given topics is sent
   * through MQTT.
   * 
   * @param topics
   * @param message
   */
  @Override
  @SimpleEvent(description = "Indicates that a given message belonging to the given topics is sent through MQTT.")
  public void MqttMessageSent(List<String> topics, String message) {
    if (DEBUG) {
      StringBuilder topicBuilder = new StringBuilder();
      for (String topic : topics) {
              topicBuilder.append(topic);
      }
      String allTopics = topicBuilder.toString();
      Log.d(LOG_TAG, "Mqtt Message " + message + " sent on subjects " + allTopics + ".");
    }
    if (topics != null && topics.size() > 0 && message != null && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MqttMessageSent", topics, message);
    }
  }

  /**
   * Indicates that the MQTT connection was lost.
   * 
   * @param error
   */
  @Override
  @SimpleEvent(description = "Indicates that the MQTT connection was lost.")
  public void MqttConnectionLost(String error) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection via MQTT lost due to this error: " + error);
    }
    if (error != null) {
      EventDispatcher.dispatchEvent(this, "MqttConnectionLost", error);
    }
  }

  @Override
  public String toString() {
    return "RaspberryPiServer[ipv4Address:" + serverAddress + ", port:" + port + ", model:" + model + ", pins:" + pins
        + ", qos:" + qos + "]";
  }

  protected String getInternalTopic() {
    StringBuilder topicBuilder = new StringBuilder();
    topicBuilder.append(Topic.INTERNAL.toString());
    topicBuilder.append(identifier);
    return topicBuilder.toString();
  }

}