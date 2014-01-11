package br.ufes.inf.lprm.sinos.publisher;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallbackImpl;
import br.ufes.inf.lprm.sinos.publisher.callback.SituationHolderImpl;
import br.ufes.inf.lprm.sinos.publisher.handler.PublisherRequestHandler;
import br.ufes.inf.lprm.situation.SituationType;

/**
 * 
 * This class creates a channel for publishing situations of a specific type in a specific situation service.
 *
 */
public abstract class SituationChannel{

	private String channelId = null;
	private PublisherRequestHandler channelHandler = null;
	private Logger logger;
	private Class<? extends SituationType> situationType;
	private PublisherCallbackImpl publisher;
	private String publisherId;
	
	/**
	 * Connects to a channel for the specified situation type, whose situation service is located at the provided host and port.
	 * If the channel does not exist, a new one is automatically created if allowed by the specified situation service.
	 * Log level is initialized as SEVERE
	 * @param host The service's address (URL or IP).
	 * @param port The service's port number.
	 * @param situationType Type of situation to be published through this connection.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public SituationChannel(String host, int port, Class<? extends SituationType> situationType) throws RemoteException, NotBoundException{
		this(null, host, port, situationType, Level.SEVERE);
	}
	
	/**
	 * Connects to a channel for the specified situation type, whose situation service is located at the provided host and port.
	 * If the channel does not exist a new one is automatically created if allowed by the specified situation service.
	 * Log level is initialized as SEVERE
	 * @param publisherId A string that identifies the publisher. If null, the service assigns a random id as the publisher's name.
	 * @param host The service's address (URL or IP).
	 * @param port The service's port number.
	 * @param situationType Type of situation to be published through this connection.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public SituationChannel(String publisherId, String host, int port, Class<? extends SituationType> situationType) throws RemoteException, NotBoundException{
		this(publisherId, host, port, situationType, Level.SEVERE);
	}
	
	/**
	 * Connects to a channel for the specified situation type, whose situation service is located at the provided host and port.
	 * If the channel does not exist a new one is automatically created if allowed by the specified situation service.
	 * @param publisherId A string that identifies the publisher. If null, the service assigns a random id as the publisher's name.
	 * @param host The service's address (URL or IP).
	 * @param port The service's port number.
	 * @param situationType Type of situation to be published through this connection.
	 * @param logLevel Level of logger output. It can be: SEVERE (errors and exceptions), WARNING (warnings and exceptions) or INFO (debug-level output).
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public SituationChannel(String publisherId, String host, int port, Class<? extends SituationType> situationType, Level logLevel) throws RemoteException, NotBoundException{
		this.publisherId = publisherId;
		this.situationType = situationType;
		ObjectStreamClass osc = ObjectStreamClass.lookup(situationType);
		String cn = osc.getName() + "&%&";
		for(ObjectStreamField field : osc.getFields()){
			cn += field.getName();
			cn += field.getTypeString();
			cn += field.getTypeCode();
		}
		
		this.channelId = cn;
		
		Registry registry = LocateRegistry.getRegistry(host, port);
		PublisherRequestHandler channelHandler = (PublisherRequestHandler) registry.lookup(PublisherRequestHandler.BIND_NAME + port);
		
		this.publisher = new PublisherCallbackImpl(this);
		channelHandler.connect(channelId, publisher, publisherId);
		this.channelHandler = channelHandler;
		
		logger = Logger.getLogger("br.ufes.inf.lprm.sinos.provider.channel.EventChannel");
		logger.setLevel(logLevel);
		
		logger.log(Level.INFO, "Provider event channel is ready establish communication with host " + host + " on port " + port);
	}
	
	/**
	 * Closes the connection with this situation channel, if it is open.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public final void disconnect () throws RemoteException {
		channelHandler.disconnect(this.publisher, channelId);
		logger.log(Level.INFO, "The channel " + channelId + " has been disconnected.");
	}
	
	/**
	 * Publishes a situation in this situation channel.
	 * @param situation The situation to be published.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public final void publish (final SituationType situation) throws RemoteException{
		if(!situationType.equals(situation.getClass())) throw new RemoteException ("Incorrect Type of Situation. Expected: " + situationType + ". Got: " + situation.getClass());
		new Thread () {
			public void run () {
				try {
					channelHandler.publish(channelId, new SituationHolderImpl(situation));
					try {sleep(1000);} catch (InterruptedException e) {}
				} catch (RemoteException e) {
					logger.log(Level.SEVERE, "Could not publish situation.", e);
				}
			}
		}.start();
	}
	
	/**
	 * This method returns a list of existing situation channels in the connected situation service. 
	 * @return An array of channel ids.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public ArrayList<String> getChannels () throws RemoteException {
		return channelHandler.getChannels();
	}
	
	/**
	 * Indicates whether the connected situation service allows publishers to create new channels.
	 * @return True if publishers are allowed to create new channels in the respective service. False otherwise. 
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public boolean canCreateChannel () throws RemoteException {
		return channelHandler.canCreateChannel();
	}
	
	public final String getId () {
		return this.publisherId;
	}
	
	public abstract void onDisconnection (DisconnectionReason reason);

}
