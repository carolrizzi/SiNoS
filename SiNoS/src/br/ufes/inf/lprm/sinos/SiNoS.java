package br.ufes.inf.lprm.sinos;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.channel.handler.CommonRequestHandler;
import br.ufes.inf.lprm.sinos.channel.handler.PublisherRequestHandlerImpl;
import br.ufes.inf.lprm.sinos.channel.handler.SubscriberRequestHandlerImpl;
import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.publisher.handler.PublisherRequestHandler;
import br.ufes.inf.lprm.sinos.subscriber.handler.SubscriberRequestHandler;

public class SiNoS {

	private Registry registry;
	private int port = Registry.REGISTRY_PORT;

	private PublisherRequestHandlerImpl providerChannel;
	private SubscriberRequestHandlerImpl consumerChannel;
	
	public static void main (String [] args){
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("sinos.properties"));
		} catch (SecurityException | IOException e) {
			System.err.println("[ERROR] Could not start Logger.");
			e.printStackTrace();
		}
		
		int port = Registry.REGISTRY_PORT;
		
		try{
			if(args.length > 0) {
				port = Integer.parseInt(args[0]);
			}else{
				port = Integer.parseInt(CommonRequestHandler.properties.getProperty("port", "" + port));
			}
		}catch (Exception e) {
			port = Registry.REGISTRY_PORT;
			Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Could not read port. Assigning default value to port (" + port + ")");
		}
		
		SiNoS manager = new SiNoS();
		try {
			manager.start(port);
		} catch (RemoteException e) {
			Logger.getLogger(SiNoS.class.getName()).log(Level.SEVERE, "Could not start Sinos.", e);
			System.exit(0);
		}
		
		new Menu(manager);
	}
	
	public void disableCreation () {
		providerChannel.ALLOW = false;
	}
	
	public void enableCreation () {
		providerChannel.ALLOW = true;
	}
	
	public void changeChannelCreationPermission () {
		providerChannel.ALLOW = !providerChannel.ALLOW;
	}
	
	public boolean creationStatus () {
		return providerChannel.ALLOW;
	}
	
	public void start (int port) throws RemoteException {
		this.port = port;
		providerChannel = new PublisherRequestHandlerImpl();
		PublisherRequestHandler channelProvider = (PublisherRequestHandler) UnicastRemoteObject.exportObject(providerChannel, 0);
		
		consumerChannel = new SubscriberRequestHandlerImpl();
		SubscriberRequestHandler channelConsumer = (SubscriberRequestHandler) UnicastRemoteObject.exportObject(consumerChannel, 0);
		
		registry = getRegistry(port);
		try {
			registry.bind(PublisherRequestHandler.BIND_NAME + port, channelProvider);
			Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, "Provider's Event Channel was bound to Sinos.");  
		} catch (AlreadyBoundException e) {
			Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Provider's Event Channel is already running on port " + port + ". Cannot rebind it.");
		}
		
		try {
			registry.bind(SubscriberRequestHandler.BIND_NAME + port, channelConsumer);
			Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, "Consumer's Event Channel was bound to Sinos.");
		} catch (AlreadyBoundException e) {
			Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Consumer's Event Channel is already running  on port " + port + ". Cannot rebind it.");
		}
		
		System.out.println("Sinos is ready and running on port " + port);
	}
	
	public void stop () {
		Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, "Stopping Sinos...");
		CommonRequestHandler.closeAllChannels(DisconnectionReason.SINOS_OFF);
		
		if(registry != null) {
			try {
				registry.unbind(PublisherRequestHandler.BIND_NAME + port);
			} catch (RemoteException | NotBoundException e) {
				Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Could not unbind provider's Event Channel.", e);
			}
			try {
				registry.unbind(SubscriberRequestHandler.BIND_NAME + port);
			} catch (RemoteException | NotBoundException e) {
				Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING,"Could not unbind consumer's Event Channel.", e); 
			}
		}
		try {
			UnicastRemoteObject.unexportObject(this.providerChannel, true);
			Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, "Stopping provider's event channel.");
		} catch (NoSuchObjectException e) {
			Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Provider channel is not running.");
		}
		
		try{
			UnicastRemoteObject.unexportObject(this.consumerChannel, true);
			Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, "Stopping consumer's event channel.");
		} catch (NoSuchObjectException e) {
			Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Consumer channel is not running.");
		}
		
		Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, "Sinos has been shut down.");
		System.exit(0);
	}
	
	private static Registry getRegistry(int port) throws RemoteException {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry();
			registry.list();  
		}
		catch (RemoteException e) { 
			registry = LocateRegistry.createRegistry(port);
		}
		return registry;
	}
	
}
