package br.ufes.inf.lprm.sinos.publisher;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallbackImpl;
import br.ufes.inf.lprm.sinos.publisher.callback.SituationHolderImpl;
import br.ufes.inf.lprm.sinos.publisher.handler.PublisherRequestHandler;
import br.ufes.inf.lprm.situation.SituationType;

/**
 * 
 * This class creates a channel for publishing situations of a specific type in a specific SiNoS Service.
 *
 */
public abstract class SituationChannel{

	private String channelName = null;
	private PublisherRequestHandler eventChannel = null;
	private Logger logger;
	private Class<? extends SituationType> situationType;
	
	/**
	 * Creates a new Situation Channel in the specified host and port. The new channel will publish situations only of the specified situation type.
	 * Log level is initialized as SEVERE
	 * @param host Address (URL or IP) of the SiNoS Service that will publish the situations.
	 * @param port Port number of the SiNoS Service that will publish the situations
	 * @param situationType Type of situation which will be published by this situation channel.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public SituationChannel(String host, int port, Class<? extends SituationType> situationType) throws RemoteException, NotBoundException{
		this(host, port, situationType, Level.SEVERE);
	}
	
	/**
	 * Creates a new Situation Channel in the specified host and port. The new channel will publish situations only of the specified situation type.
	 * Log level is initialized as SEVERE
	 * @param host Address (URL or IP) of the SiNoS Service that will publish the situations.
	 * @param port Port number of the SiNoS Service that will publish the situations
	 * @param situationType Type of situation which will be published by this situation channel.
	 * @param logLevel Level of logger output. It can be: SEVERE (errors and exceptions), WARNING (warnings and exceptions) or INFO (debug-level output).
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 * @throws NotBoundException Indicates that there is no event channel running in the provided host.
	 */
	public SituationChannel(String host, int port, Class<? extends SituationType> situationType, Level logLevel) throws RemoteException, NotBoundException{
		this.situationType = situationType;
		ObjectStreamClass osc = ObjectStreamClass.lookup(situationType);
		String cn = osc.getName() + "&%&";
		for(ObjectStreamField field : osc.getFields()){
			cn += field.getName();
			cn += field.getTypeString();
			cn += field.getTypeCode();
		}
		
		this.channelName = cn;
		
		Registry registry = LocateRegistry.getRegistry(host, port);
		PublisherRequestHandler eventChannel = (PublisherRequestHandler) registry.lookup(PublisherRequestHandler.BIND_NAME + port);
		
		eventChannel.createChannel(channelName, new PublisherCallbackImpl(this));
		this.eventChannel = eventChannel;
		
		logger = Logger.getLogger("br.ufes.inf.lprm.sinos.provider.channel.EventChannel");
		logger.setLevel(logLevel);
		
		logger.log(Level.INFO, "Provider event channel is ready establish communication with host " + host + " on port " + port);
	}
	
	/**
	 * Closes this situation channel, if it is open.
	 * @throws RemoteException Indicates the occurrence of a communication-related exception during the execution of this remote method call.
	 */
	public final void closeChannel () throws RemoteException {
		eventChannel.closeChannel(this.hashCode(), channelName);
		logger.log(Level.INFO, "The channel " + channelName + " has been shut down.");
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
					eventChannel.publishSituation(channelName, new SituationHolderImpl(situation));
					try {sleep(1000);} catch (InterruptedException e) {}
				} catch (RemoteException e) {
					logger.log(Level.SEVERE, "Could not publish situation.", e);
				}
			}
		}.start();
	}
	
	
	public abstract void onDisconnection (DisconnectionReason reason);

}
