package br.ufes.inf.lprm.sinos.channel;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.channel.handler.CommonRequestHandler;
import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;

public class PublisherChannelManager {

//	private ArrayList<PublisherCallback> publishers = new ArrayList<>();
	private HashMap<PublisherCallback, String> publishers = null;
	
	public PublisherChannelManager() {
		publishers = new HashMap<>();
	}
	
	public synchronized void connectPublisher (PublisherCallback publisher, String publisherId) throws RemoteException {
		if (publisher == null) {
			throw new RemoteException("Invalid publisher callback. Publisher callback cannot be null.");
		}
		if(publisherId == null || publisherId.isEmpty()){
			publisherId = (UUID.randomUUID()).toString();
		}
		publishers.put(publisher, publisherId);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Publisher connected: " + publisherId);
	}
	
	public synchronized void disconnectAllPublishers (DisconnectionReason reason){
		if(reason == null){
			reason = DisconnectionReason.UNKNOWN;
		}
		for(Entry<PublisherCallback, String> publisher : publishers.entrySet()){
			sendDisconnectionNotification(publisher.getKey(), publisher.getValue(),  reason);
		}
		publishers = new HashMap<>();
	}
	
	public synchronized void disconnectPublisher (PublisherCallback publisher, DisconnectionReason reason) throws RemoteException {
		String id = publishers.remove(publisher);
		if(id == null) {
			throw new RemoteException("Could not notify disconnection to publisher, because publisher does not exist.");
		} else {
			if(reason != null)
				sendDisconnectionNotification(publisher, id, reason);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Publisher disconnected: " + id);
		}
	}

	public void listPublishers() {
		if(publishers.isEmpty()){
			System.out.println("There are currently no publishers registered for this channel.");
			return;
		}
		for(String publisherId : publishers.values())
			System.out.println("- " + publisherId);
	}
	
	private void sendDisconnectionNotification (final PublisherCallback publisher, final String id, final DisconnectionReason reason) {
		if (reason == null) return;
		new Thread () {
			public void run() {
				boolean success = false;
				for(int i = 0; i < CommonRequestHandler.attempts; i++){
					try{
						publisher.disconnect(reason);
						break;
					}catch (Exception e) {
						Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Attempt " + (i + 1) + " of reaching publisher " + id + ".");
					}
				}
				if(!success){
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not notify disconnection to publisher " + id + ", because publisher is unreacheable");
				}
			}
		}.start();
	}
	
}
