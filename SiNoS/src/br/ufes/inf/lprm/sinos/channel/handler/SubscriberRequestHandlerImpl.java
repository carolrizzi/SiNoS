package br.ufes.inf.lprm.sinos.channel.handler;

import java.rmi.RemoteException;
import java.util.ArrayList;

import br.ufes.inf.lprm.sinos.channel.ChannelManager;
import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;
import br.ufes.inf.lprm.sinos.subscriber.handler.SubscriberRequestHandler;

public class SubscriberRequestHandlerImpl extends CommonRequestHandler implements SubscriberRequestHandler{

	@Override
	public void subscribe(SubscriberCallback subscriber, String subscriberId) throws RemoteException {
		ChannelManager channel = getChannel(subscriber);
		channel.subscribe(subscriber, subscriberId);
	}

	@Override
	public void unsubscribe(SubscriberCallback subscriber) throws RemoteException{
		ChannelManager channel = getChannel(subscriber);
		channel.unsubscribe(subscriber);
	}
	
	@Override
	public ArrayList<String> getChannels() throws RemoteException {
		return CommonRequestHandler.getChannelsList();
	}
	
	private ChannelManager getChannel (SubscriberCallback callback) throws RemoteException{
		if(callback == null) throw new RemoteException("Invalid Situation Listener. Callback cannot be null.");
		
		String channelId = null;
		if((channelId = callback.getChannelId()) == null || channelId.equals(""))
			throw new RemoteException("Invalid Situation Channel Id");
		
		ChannelManager channel =  channels.get(channelId);
		if(channel == null) throw new RemoteException("Situation Channel not found: " + channelId);
		
		return channel;
	}
}
