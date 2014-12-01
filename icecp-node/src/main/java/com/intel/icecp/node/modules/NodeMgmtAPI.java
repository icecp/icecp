//package com.intel.icecp.node.modules;
//
//import java.io.NotSerializableException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
//
//import com.intel.icecp.core.Channel;
//import com.intel.icecp.core.Module;
//import com.intel.icecp.core.Message;
//import com.intel.icecp.core.Node;
//import com.intel.icecp.request_response.impl.HashResponder;
//import com.intel.icecp.core.messages.BytesMessage;
//import com.intel.icecp.core.metadata.Persistence;
//import com.intel.icecp.core.metadata.formats.BytesFormat;
//import com.intel.icecp.core.misc.ChannelIOException;
//import com.intel.icecp.core.misc.ChannelLifetimeException;
//import com.intel.icecp.core.misc.Configuration;
//import com.intel.icecp.request_response.OnRequest;
//import com.intel.icecp.node.messages.MQTTMessage;
//import com.intel.icecp.node.messages.NodeMgmtAPIMessage;
//import com.intel.icecp.core.operations.OperationsPipeline;
//import com.intel.icecp.node.operations.FormattingOperation;
//import com.intel.icecp.node.utils.ChannelUtils;
//import java.io.InputStream;
//
///**
// *	This feature is a core feature that will get loaded
// *	on each node.  It is responsible for taking requests regarding
// *	features (eg, feature status, load a feature, stop a feature, etc).
// *
// *	See the NodeMgmtAPIMessage class for details on commands and 
// *	parameters.
// *
// *
// */
//public class NodeMgmtAPI implements Module {
//
//	private static final Logger logger = LogManager.getLogger();
//	private boolean verbose = true;
//	
//	/**
//	 * This is the name of this feature and the name of the channel
//	 * that is subscribes to for receiving commands.
//	 */
//	private String name = "NodeMgmtAPI";
//	private static final String NODEMGMTAPI_INFO_CHANNEL_NAME = "node-mgmt-info";
//
//	private static final int NODEMGMTAPI_INFO_LIFETIME_MS = 1000;
//
//	//TODO: This will eventually go away.
//	private static final int SLEEPTIME_BEFORE_CLOSE_MS = 2000;
//	
//	private Node node = null;
//	private Channel<Status> statusChannel = null;
//	private Configuration featureConfiguration = null;
//	private Channel<NodeMgmtAPIMessage> infoChannel = null;
//	private boolean stopped = false;
//	private Channel<BytesMessage> jarChannel = null;
//	
//	public class OnRequestHandler implements OnRequest<NodeMgmtAPIMessage, NodeMgmtAPIMessage> {
//
//		@Override
//		public NodeMgmtAPIMessage onRequest(NodeMgmtAPIMessage request) {
//			logger.info(String.format("Info Channel Received:  Cmd[%s] FeatureName[%s]", request.command, request.featureName));
//
//			NodeMgmtAPIMessage retMessage = new NodeMgmtAPIMessage(request.command);
//			
//			if (request.command == NodeMgmtAPIMessage.COMMAND.GET_ALL_CHANNELS) {
//				URI [] uris = node.getChannelNames();
//				//logger.info(String.format("GetChannels found %d channels", uris.length));
//				String [] strURIs = new String[uris.length];
//				for (int i=0; i<uris.length; i++) {
//					strURIs[i] = uris[i].toString();
//				}
//				retMessage.returnArray = strURIs;
//				retMessage.returnStatus = "OK";
//			}
//			
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.GET_ALL_FEATURE_NAMES) {
//				retMessage.returnArray = node.getModuleNames();
//				retMessage.returnStatus = "OK";
//			}
//			
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.GET_ALL_FEATURE_STATUSES) {
//				String[] featureNames = node.getModuleNames();
//				int numFeatures = featureNames.length;
//				String[] featureStatuses = new String[numFeatures];
//				for (int f=0; f<numFeatures; f++) {
//					try {
//						featureStatuses[f] = 
//							featureNames[f] + ":" + node.getModule(featureNames[f]).getStatusChannel().latest().get().toString();
//					} catch (Exception e) {
//						featureStatuses[f] = 
//							featureNames[f] + ":" + "Failed to get status";
//					}
//				}
//				retMessage.returnArray = featureStatuses;
//				retMessage.returnStatus = "OK";
//			}
//
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.GET_FEATURE_STATUS) {
//				try {
//					if (findFeature(request.featureName)) {
//						Module.Status status = node.getModule(request.featureName).getStatusChannel().latest().get();
//						retMessage.returnStatus = status.toString();
//					}
//					else {
//						retMessage.returnStatus = String.format("Feature [%s] not loaded", request.featureName);
//					}
//					
//				} catch (Exception e) {
//					retMessage.returnStatus = "Error accessing the feature: " + e.getMessage();
//					logger.error("Failed to get feature status.", e);
//				}
//				retMessage.featureName = request.featureName;
//			}
//			
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.LOAD_FEATURE) {
//				try {
//					//open a channel to the jar file
//					jarChannel = node.openChannel(
//							request.featureJarChannel,
//							new OperationsPipeline(
//									BytesMessage.class, 
//									InputStream.class, 
//									new FormattingOperation(new BytesFormat())),
//							BytesMessage.class, 
//							new Persistence(), 
//							new BytesFormat());
//					//pass channel into node.loadfeature()
//					node.loadModule(jarChannel);
//					//return status
//					retMessage.returnStatus = "OK";
//				}
//				catch (Exception ex) {
//					retMessage.returnStatus = "Failed to load feature.  " + ex.getMessage();
//					logger.error("Failed to load feature.", ex);
//				}
//				retMessage.featureName = request.featureName;
//			}
//			
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.STOP_FEATURE) {
//				try {
//					if (findFeature(request.featureName)) {
//						node.stopModule(request.featureName);
//						retMessage.returnStatus = "OK";
//					}
//					else
//						retMessage.returnStatus = String.format("Feature [%s] not loaded", request.featureName);
//					
//				} catch (Exception e) {
//					retMessage.returnStatus = "Failed to shutdown feature: " + e.getMessage();
//					logger.error("Failed to shutdown feature.", e);
//				}
//				retMessage.featureName = request.featureName;
//			}
//			
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.PUBLISH_TO_MQTT_BROKER) {
//				try {
//					//Get the MQTTBridge module
//					Module module = node.getModule("MQTTBridge");	//this is hard coded, see the actual MQTTBridge module to verify
//					String brokerTopic = ((MQTTBridge)module).getBrokerTopic();
//					String [] brokerURIs = ((MQTTBridge)module).getBrokerURIs();
//					
//				    try {
//			        	MqttDefaultFilePersistence filePersistence = new MqttDefaultFilePersistence(getCacheFolder("~/.mqttCache"));
//			        	
//			        	MqttClient client = new MqttClient(brokerURIs[0], "NodeAPI_ClientId", filePersistence);
//			        	
//						MqttConnectOptions connectOptions = new MqttConnectOptions();
//						connectOptions.setServerURIs(brokerURIs);
//
//						client.connect(connectOptions);
//				        MqttMessage message = new MqttMessage();
//				        message.setPayload(request.other.getBytes());
//				        client.publish(brokerTopic, message);
//				        retMessage.returnStatus = "OK";
//				        retMessage.featureName = module.getName();
//				    } catch (MqttException e) {
//				        e.printStackTrace();
//				    }
//					
//				} catch (Exception e) {
//					retMessage.returnStatus = "Failed to get MQTTBridge module: " + e.getMessage();
//					logger.error("Failed to get MQTTBridge module.", e);
//				}
//			}
//			
//			else if (request.command == NodeMgmtAPIMessage.COMMAND.GET_MQTT_BROKER_CHANNEL_LATEST) {
//				retMessage.command = request.command;
//				retMessage.featureName = request.featureName;
//				
//				try {
//					Channel<MQTTMessage> mqttChannel = node.openChannel(new URI(request.other), MQTTMessage.class, new Persistence());
//					MQTTMessage msg = mqttChannel.latest().get(5000, TimeUnit.MILLISECONDS);
//					retMessage.other = msg.toString();
//					retMessage.returnStatus = "OK";
//				} catch (ChannelLifetimeException | InterruptedException | ExecutionException | ChannelIOException e) {
//					retMessage.returnStatus = "Error on the channel";
//					logger.error(String.format("Error getting latest on [%s]: Error: %s", request.other, e.getMessage()));
//				} catch (URISyntaxException e) {
//					retMessage.returnStatus = "BAD URI";
//				} catch (TimeoutException e) {
//					retMessage.returnStatus = "Timed Out";
//				}
//			}
//			
//			else {
//				logger.info("Command Not Supported: " + request.command);
//				retMessage.returnStatus = "Command Not Supported";
//			}
//
//			return retMessage;
//		}//handler
//	}
//	
//	private String getCacheFolder(String folderPath) {
//		String fixedPath = folderPath;
//		if (folderPath.contains("~")) {
//			String userHome = System.getProperty("user.home");
//			fixedPath = folderPath.replace("~", userHome);
//		}
//		return fixedPath.replace("\\", "/");
//	}
//	
//	@Override
//	public void run() {
//		
//		try {
//			
//			statusChannel.publish(Module.Status.RUNNING);
//			
//			HashResponder<NodeMgmtAPIMessage, NodeMgmtAPIMessage> commandResponder = new HashResponder<NodeMgmtAPIMessage, NodeMgmtAPIMessage>(
//				node.channels(), NodeMgmtAPIMessage.class, NodeMgmtAPIMessage.class);
//			
//			URI nodeMgmtAPIChannel = URI.create(node.getStatusChannel().getName() + "/" + NODEMGMTAPI_INFO_CHANNEL_NAME);
//			
//			if (verbose)
//				logger.info("Create the HashResponder, URI:" + nodeMgmtAPIChannel.toString());
//
//			OnRequestHandler requestHandler = new OnRequestHandler();
//			
//			if (verbose)
//				logger.info("Setup the response");
//			commandResponder.listen(nodeMgmtAPIChannel, requestHandler);
//
//			logger.info("Command Channel ready!");
////			while (!stopped) {
////				//logger.info("Sleeping");
////				Thread.sleep(500);
////			}
//			
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//	}
//	
//	/**
//	 * Utility method to see if a specified feature is loaded.
//	 * @param featureName - the feature to look for
//	 * @return true=feature found, false=not found
//	 */
//	private boolean findFeature(String featureName) {
//		boolean found = false;
//		String[] names = node.getModuleNames();
//		for (String name : names) {
//			if (name.equals(featureName)) {
//				found = true;
//				break;
//			}
//		}
//		return found;
//	}
//	
//	/**
//	 * Create the channel to use, for the response.  Use the utility method to create the channel
//	 * name, using the message.
//	 * @param message - required to make the correct channel name
//	 * @return - the return channel to use.
//	 */
//	private Channel<Message> createReturnChannel( Message message) {
//		
//		Channel<Message> retChannel = null;
//		try {
//			URI uri = ChannelUtils.getResponseChannelUri(infoChannel.getName(), message);
//			retChannel = node.openChannel(uri, null, Message.class, new Persistence(NODEMGMTAPI_INFO_LIFETIME_MS));
//			if (verbose)
//				logger.info("Return Channel created: " + retChannel.getName());
//		} catch (ChannelLifetimeException | NotSerializableException | URISyntaxException ex) {
//			logger.error(ex);
//			retChannel = null;
//		}
//		return retChannel;
//	}
//
//	@Override
//	public String getName() {
//		return name;
//	}
//
//	@Override
//	public Channel<Status> getStatusChannel() {
//		return statusChannel;
//	}
//
//	@Override
//	public void onStartUp(Node node, Channel<Status> statusChannel, Configuration featureConfiguration)  {
//		logger.info("Started.");
//		this.node = node;
//		this.statusChannel = statusChannel;
//		this.featureConfiguration = featureConfiguration;
//		try {
//			statusChannel.publish(Module.Status.ENABLED);
//		} catch (ChannelIOException e) {
//			logger.error("onStartUp - Failed to publish status", e);
//		}
//	}
//
//	@Override
//	public void shutdown() {
//		try {
//			logger.info("Stopped.");
//			stopped = true;
//			statusChannel.publish(Module.Status.OFF);
//			//TODO:  Shouldn't have to sleep.
//			Thread.sleep(SLEEPTIME_BEFORE_CLOSE_MS);
//			statusChannel.close();
//			
//		} catch (ChannelIOException | InterruptedException | ChannelLifetimeException e) {
//			logger.error("shutdown - Error during close", e);
//		}
//
//	}
//
//}
