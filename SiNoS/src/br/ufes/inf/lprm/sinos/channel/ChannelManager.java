package br.ufes.inf.lprm.sinos.channel;

import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;
import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;

public class ChannelManager {

	private String id;
	private String name;
	private PublisherChannelManager pubManager;
	private SubscriberChannelManager subManager;
	
	public ChannelManager(String channelId, String channelName) {
		this.id = channelId;
		this.name = channelName;
		pubManager = new PublisherChannelManager();
		subManager = new SubscriberChannelManager();
	}
	
	// --- CHANNEL --- //
	public String getChannelId () {
		return this.id;
	}
	
	public String getChannelName () {
		return this.name;
	}
	
	// --- PUBLISHER --- //
	public void connectPublisher (PublisherCallback publisher, String publisherId) throws RemoteException {
		pubManager.connectPublisher(publisher, publisherId);
	}
	
	public void disconnectPublisher (PublisherCallback publisher, DisconnectionReason reason) throws RemoteException {
		pubManager.disconnectPublisher(publisher, reason);
	}
	
	public void disconnectChannel (DisconnectionReason reason) {
		pubManager.disconnectAllPublishers(reason);
		subManager.disconnectAllSubscribers(reason);
	}

	public void listPublishers() {
		System.out.println("\nChannel " + this.name + ": ");
		pubManager.listPublishers();
	}
	
	// --- SUBSCRIBER --- //
	public void subscribe (SubscriberCallback subscriber, String subscriberId) throws RemoteException {
		subManager.subscribe(subscriber, subscriberId);
	}
	
	public void unsubscribe (SubscriberCallback subscriber) throws RemoteException {
		subManager.unsubscribe(subscriber);
	}
	
	public void publish (NotificationType notificationType, SituationHolder situation) {
		subManager.publish(notificationType, situation);
	}
	
	public void listSubscribers () {
		System.out.println("\nChannel " + this.name + ": ");
		subManager.listSubscribers();
	}
}
