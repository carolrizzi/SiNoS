package br.ufes.inf.lprm.sinos.channel.handler;

import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.channel.ChannelManager;
import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;
import br.ufes.inf.lprm.sinos.subscriber.handler.SubscriberRequestHandler;

public class SubscriberRequestHandlerImpl extends CommonRequestHandler implements SubscriberRequestHandler{

	@Override
	public void subscribe(SubscriberCallback subscriber) throws RemoteException {
		ChannelManager channel = getChannel(subscriber);
		channel.subscribe(subscriber);
	}

	@Override
	public void unsubscribe(SubscriberCallback subscriber) throws RemoteException{
		ChannelManager channel = getChannel(subscriber);
		channel.unsubscribe(subscriber);
	}
	
	private ChannelManager getChannel (SubscriberCallback callback) throws RemoteException{
		if(callback == null) throw new RemoteException("Invalid Situation Listener.");//CallbackException(CallbackException.Message.INVALID_CALLBACK);
		
		String channelName = null;
		if((channelName = callback.getEventChannel()) == null || channelName.equals(""))
			throw new RemoteException("Invalid Situation Channel.");//SituationChannelException(SituationChannelException.Message.INVALID_EVENT_CHANNEL);
		
		ChannelManager channel =  channels.get(channelName);
		if(channel == null) throw new RemoteException("Situation Channel not found.");//SituationChannelException(SituationChannelException.Message.CHANNEL_NOT_FOUND);
		
		return channel;
	}
}
