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
 * This class provides means to communicate with a SiNoS Service in order to listen situation nofitications.
 *
 */
public class ServiceConnection{
	private static final String ACTIVATION = "onSituationActivation";
	private static final String DEACTIVATION = "onSituationDeactivation";
	
	private SubscriberRequestHandler channel = null;
	private HashMap<SituationListener<?>, ArrayList<SubscriberCallback>> handlers;
	private Logger logger;
	
	/**
	 * Connects to the SiNoS Service that is available in the specified host and port.
	 * Log level is initialized as SEVERE.
	 * @param host Address (URL or IP) of the SiNoS Service.
	 * @param port Port number of the SiNoS Service.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public ServiceConnection(String host, int port) throws RemoteException, NotBoundException{
		this(host, port, Level.SEVERE);
	}
	
	/**
	 * Connects to the SiNoS Service that is available in the specified host and port. 
	 * @param host Address (URL or IP) of the SiNoS Service.
	 * @param port Port number of the SiNoS Service.
	 * @param logLevel Level of logger output (SEVERE = errors and exceptions; WARNING = warnings and exceptions; INFO = debug-level output).
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public ServiceConnection(String host, int port, Level logLevel) throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(host, port);
		channel = (SubscriberRequestHandler) registry.lookup(SubscriberRequestHandler.BIND_NAME + port);
		handlers = new HashMap<SituationListener<?>, ArrayList<SubscriberCallback>>();
		
		logger = Logger.getLogger("br.ufes.inf.lprm.sinos.consumer.channel.EventChannel");
		logger.setLevel(logLevel);
		
		logger.log(Level.INFO, "Consumer event channel is ready establish communication with host " + host + " on port " + port);
	}
	
	/**
	 * This method subscribes a listener in the connected SiNoS Service. 
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
				SubscriberCallback callback = new ActivationCallback<T>(listener, notifyDisconnection);
				channel.subscribe(callback);
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
				SubscriberCallback callback = new DeactivationCallback<T>(listener, notifyDisconnection);
				channel.subscribe(callback);
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
				SubscriberCallback callback = new MirroringCallback<T>(listener, notifyDisconnection);
				channel.subscribe(callback);
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
	 * This method unsubscribes a listener.
	 * @param listener The listener to be unsubscribed.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public <T extends SituationType> void unsubscribe (SituationListener<T> listener) throws RemoteException {
		if(listener == null) return;
		ArrayList<SubscriberCallback> callbacks = handlers.remove(listener);
		if(callbacks == null) return;
		
		for(SubscriberCallback callback : callbacks){
			try {
				channel.unsubscribe(callback);
				logger.log(Level.INFO, "Event handler and its callbacks were unsubscribed from channel " + listener.getChannelName());
			} catch (RemoteException e) {
				logger.log(Level.SEVERE, "Could not unsubscribe event handler.", e);
			}
		}
	}
	
	/**
	 * This method disables/unsubscribes a specific notification type of a listener.
	 * @param listener The listener that contains the notification type that will be disabled.
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
