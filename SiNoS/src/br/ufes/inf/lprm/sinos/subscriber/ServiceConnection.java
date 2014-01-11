package br.ufes.inf.lprm.sinos.subscriber;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.subscriber.callback.ActivationCallback;
import br.ufes.inf.lprm.sinos.subscriber.callback.DeactivationCallback;
import br.ufes.inf.lprm.sinos.subscriber.callback.MirroringCallback;
import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;
import br.ufes.inf.lprm.sinos.subscriber.handler.SubscriberRequestHandler;
import br.ufes.inf.lprm.situation.SituationType;

/**
 * 
 * This class provides means to communicate with a situation service in order to listen situation notifications.
 *
 */
public class ServiceConnection{
	private static final String ACTIVATION = "onSituationActivation";
	private static final String DEACTIVATION = "onSituationDeactivation";
	
	private SubscriberRequestHandler channel = null;
	private HashMap<SituationListener<?>, ArrayList<SubscriberCallback>> handlers;
	private Logger logger;
	private String subscriberName;
	
	/**
	 * Connects to the situation service located at the specified host and port.
	 * Log level is initialized as SEVERE.
	 * @param host The service's address (URL or IP).
	 * @param port The service's port number.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no situation channel running in the provided host.
	 */
	public ServiceConnection(String host, int port) throws RemoteException, NotBoundException{
		this(null, host, port, Level.SEVERE);
	}
	
	/**
	 * Connects to the situation service located at the specified host and port.
	 * Log level is initialized as SEVERE.
	 * @param subscriberName A string that identifies the subscriber. If null, the service assigns a random id as the subscriber's name.
	 * @param host The service's address (URL or IP).
	 * @param port The service's port number.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no situation channel running in the provided host.
	 */
	public ServiceConnection(String subscriberName, String host, int port) throws RemoteException, NotBoundException{
		this(subscriberName, host, port, Level.SEVERE);
	}
	
	/**
	 * Connects to the situation service located at the specified host and port.
	 * @param subscriberName A string that identifies the subscriber. If null, the service assigns a random id as the subscriber's name.
	 * @param host The service's address (URL or IP).
	 * @param port The service's port number.
	 * @param logLevel Level of logger output (SEVERE = errors and exceptions; WARNING = warnings and exceptions; INFO = debug-level output).
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public ServiceConnection(String subscriberName, String host, int port, Level logLevel) throws RemoteException, NotBoundException {
		this.subscriberName = subscriberName;
		Registry registry = LocateRegistry.getRegistry(host, port);
		channel = (SubscriberRequestHandler) registry.lookup(SubscriberRequestHandler.BIND_NAME + port);
		handlers = new HashMap<SituationListener<?>, ArrayList<SubscriberCallback>>();
		
		logger = Logger.getLogger("br.ufes.inf.lprm.sinos.consumer.channel.EventChannel");
		logger.setLevel(logLevel);
		
		logger.log(Level.INFO, "Consumer event channel is ready establish communication with host " + host + " on port " + port);
	}
	
	/**
	 * This method subscribes a listener in the connected situation service. 
	 * @param listener The listener to be subscribed.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public <T extends SituationType> void subscribe (SituationListener<T> listener) throws RemoteException {
		if (listener == null) return;
		if(handlers.containsKey(listener)) return;
		
		ArrayList<SubscriberCallback> callbacks = new ArrayList<SubscriberCallback>();
		boolean hasCallback = false;
		boolean notifyDisconnection = true;

		if(isDeclaringClass(listener, ACTIVATION)){
			hasCallback = true;
			try {
				SubscriberCallback callback = new ActivationCallback<T>(this.subscriberName, listener, notifyDisconnection);
				channel.subscribe(callback, callback.getId());
				callbacks.add(callback);
				notifyDisconnection = false;
				logger.log(Level.INFO, "Callback for activation events was subscribed to channel " + listener.getChannelName());
			} catch (RemoteException e) {
				logger.log(Level.SEVERE, "Could not subscribe callback for activation events", e);
			}
		}
		
		if (isDeclaringClass(listener, DEACTIVATION)){
			hasCallback = true;
			try{
				SubscriberCallback callback = new DeactivationCallback<T>(this.subscriberName, listener, notifyDisconnection);
				channel.subscribe(callback, callback.getId());
				callbacks.add(callback);
				notifyDisconnection = false;
				logger.log(Level.INFO, "Callback for deactivation events was subscribed to channel " + listener.getChannelName());
			} catch (RemoteException e) {
				logger.log(Level.SEVERE, "Could not subscribe callback for deactivation events", e);
			}
		}
		
		if (listener.isMirrored()){
			hasCallback = true;
			try{
				SubscriberCallback callback = new MirroringCallback<T>(this.subscriberName, listener, notifyDisconnection);
				channel.subscribe(callback, callback.getId());
				callbacks.add(callback);
				notifyDisconnection = false;
				logger.log(Level.INFO, "Callback for events mirroring (shadow fact) was subscribed to channel " + listener.getChannelName());
			} catch (RemoteException e) {
				logger.log(Level.SEVERE, "Could not subscribe callback for mirroring (shadow fact).", e);
			}
		}
		
		if(!hasCallback) return;
		if(callbacks.size() <= 0) throw new RemoteException();
		handlers.put(listener, callbacks);
		logger.log(Level.INFO, "Event handler was registered.");
	}
	
	/**
	 * This method unsubscribes a previously subscribed listener from the connected situation service.
	 * @param listener The listener to be unsubscribed. Note that, in order to unsubscribe a listener, the same must have been previously subscribed.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public <T extends SituationType> void unsubscribe (SituationListener<T> listener) throws RemoteException {
		if(listener == null) return;
		ArrayList<SubscriberCallback> callbacks = handlers.remove(listener);
		if(callbacks == null) return;
		
		for(SubscriberCallback callback : callbacks){
			try {
				channel.unsubscribe(callback);
				logger.log(Level.INFO, "Listener was unsubscribed from channel " + listener.getChannelName());
			} catch (RemoteException e) {
				logger.log(Level.SEVERE, "Could not unsubscribe listener.", e);
			}
		}
	}
	
	/**
	 * This method disables/unsubscribes a specific notification type of a listener from the connected situation service.
	 * @param listener The listener whose notification type will be disabled.
	 * @param notificationType The type of notification to be disabled.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public <T extends SituationType> void unsubscribe (SituationListener<T> listener, NotificationType notificationType) throws RemoteException {
		if(listener == null) return;
		if(notificationType == null) {
			unsubscribe(listener);
			return;
		}
		
		ArrayList<SubscriberCallback> callbacks = handlers.get(listener);
		if(callbacks == null) return;
		SubscriberCallback callback = null;
		
		boolean exception = true;
		
		for (SubscriberCallback c : callbacks){
			try {
				if (c.getOperation().equals(notificationType)){
					callback = c;
					callbacks.remove(c);
					exception = false;
				}
			} catch (RemoteException e) {
				logger.log(Level.WARNING, "Could not check callback for remotion.", e);
			}
		}
		
		if(exception) throw new RemoteException();
		if(callback == null) return;
		
		try {
			channel.unsubscribe(callback);
			logger.log(Level.INFO, "Callback was unsubscribed from channel " + listener.getChannelName());
		} catch (RemoteException e) {
			logger.log(Level.SEVERE, "Could not unsubscribe callback.", e);
		}
		
		if(callbacks.size() <= 0){
			handlers.remove(listener);
		}
		
		callback.disconnect(null);
	}
	
	/**
	 * This method returns a list of existing situation channels in the connected situation service. 
	 * @return An array of channel ids.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public ArrayList<String> getChannels () throws RemoteException {
		return channel.getChannels();
	}
	
	private <T extends SituationType> boolean isDeclaringClass (SituationListener<T> handler, String methodName) throws SecurityException {
		Class<?> handlerClass = handler.getClass();
		
		ParameterizedType pt = (ParameterizedType) handlerClass.getGenericSuperclass();
		
		Class<?>[] parameters = new Class<?>[1];
		parameters[0] = (Class<?>) pt.getActualTypeArguments()[0];
		
		Method method = null;
		try{
			method = handlerClass.getMethod(methodName, parameters);
		}catch (NoSuchMethodException e) {
			return false;
		}
		
		return handlerClass.equals(method.getDeclaringClass());
	}
}
