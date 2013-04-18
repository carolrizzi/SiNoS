package br.ufes.inf.lprm.sinos.channel;

import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.channel.handler.CommonRequestHandler;
import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;
import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;

public class ChannelManager {

	private String channelName;
	private PublisherChannelManager publisherManager;
	private SubscriberChannelManager subscriberManager;
	
	public ChannelManager(String channelName, PublisherCallback publisher) throws RemoteException {
		this.channelName = channelName;
		publisherManager = new PublisherChannelManager(publisher, channelName, this);
		subscriberManager = new SubscriberChannelManager();
	}
	
	public void addPublisher (PublisherCallback publisher) throws RemoteException {
		publisherManager.addPublisher(publisher);
	}
	
	public void disconnect (DisconnectionReason reason) {
		publisherManager.disconnect(reason);
		subscriberManager.disconnect(reason);
	}
	
	public void closeChannel (int publisherId) {
		if(publisherManager.closeChannel(publisherId)){
			unsubscribeAllAndCloseChannel(DisconnectionReason.CHANNEL_OFF);
		}
	}
	
	public void unsubscribeAllAndCloseChannel (DisconnectionReason reason) {
		subscriberManager.disconnect(reason);
		CommonRequestHandler.closeChannel(channelName);
	}
	
	public void publish (NotificationType notificationType, SituationHolder situation) {
		subscriberManager.publish(notificationType, situation);
//		publisherManager.restart();
	}
	
	public void subscribe (SubscriberCallback subscriber) {
		subscriberManager.subscribe(subscriber);
	}
	
	public void unsubscribe (SubscriberCallback subscriber) {
		subscriberManager.unsubscribe(subscriber);
	}
	
}
