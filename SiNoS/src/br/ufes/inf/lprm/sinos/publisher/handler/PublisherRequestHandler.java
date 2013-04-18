package br.ufes.inf.lprm.sinos.publisher.handler;

import java.rmi.Remote;
import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;

/**
 * 
 * Event Channel containing methods for situation providers' usage.
 *
 */
public interface PublisherRequestHandler extends Remote{

	/**
	 * Event Channel bind name.
	 */
	public static final String BIND_NAME = "EventChannelProvider";
	
	/**
	 * Creates a new event channel with the given name.
	 * @param channelName Name of the channel to be created. If this string is null, no channel is created.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws ChannelNotFound Thrown when the given channel name is invalid, e.g., when it is null.
	 */
	public void createChannel(String channelName, PublisherCallback callback) throws RemoteException;
	
	/**
	 * Given a name of an existing event channel, disconnects each consumer connected to that channel and closes it. 
	 * @param channelName The name of the channel to be disconnected.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws ChannelNotFound Thrown when the given channel name is invalid, e.g., when it is null or does not name a existing channel.
	 */
	public void closeChannel (int id, String channelName) throws RemoteException;
	
	/**
	 * Publishes a given situation object in the specified event channel, so clients subscribed to that channel are notified.
	 * @param channelName Name of the channel in which the situation object will be published.
	 * @param situation Situation object that will be published. 
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws ChannelNotFound Thrown when the given channel name is invalid, e.g., when it is null or does not name a existing channel.
	 */
	public void publishSituation (String channelName, SituationHolder situation) throws RemoteException;

	/**
	 * Disconnects each consumer connected to each existing channel and closes the connection.
	 * We recommended to use this method in case of shutting down the situation provider server.  
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	
}
