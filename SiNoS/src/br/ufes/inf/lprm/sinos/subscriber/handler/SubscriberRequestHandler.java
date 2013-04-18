package br.ufes.inf.lprm.sinos.subscriber.handler;

import java.rmi.Remote;
import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;

/**
 * 
 * Event Channel containing methods for situation consumers' usage.
 *
 */
public interface SubscriberRequestHandler extends Remote{

	/**
	 * Event Channel bind name.
	 */
	public static final String BIND_NAME = "EventChannelClient";
	
	/**
	 * Subscribes the given callback in the event channel returned by the method getEventChannel in the callback.
	 * If the given callback is already subscribed in the specified event channel, then no action is performed.  
	 * @param callback Callback to be subscribed. Its getEventChannel method has to return the name of the target event channel where the callback should be subscribed.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws InvalidCallback Thrown when the given callback is invalid, e.g., when it is null or provides an invalid channel name.
	 */
	public void subscribe(SubscriberCallback callback) throws RemoteException;
	
	/**
	 * Removes the subscription of the given callback from the event channel returned by the method getEventChannel in the callback.
	 * If the given callback is not subscribed in the specified event channel, then no action is performed.  
	 * @param callback Callback to be unsubscribed. Its getEventChannel method has to return the name of the target event channel from where the callback should be unsubscribed.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws InvalidCallback InvalidCallback Thrown when the given callback is invalid, e.g., when it is null or provides an invalid channel name.
	 */
	public void unsubscribe (SubscriberCallback callback) throws RemoteException;
}
