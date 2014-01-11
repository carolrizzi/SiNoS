package br.ufes.inf.lprm.sinos.subscriber.handler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;

public interface SubscriberRequestHandler extends Remote{

	public static final String BIND_NAME = "SituationChannelSubscriber";
	
	public void subscribe(SubscriberCallback callback, String subscriberId) throws RemoteException;

	public void unsubscribe (SubscriberCallback callback) throws RemoteException;

	ArrayList<String> getChannels() throws RemoteException;
}
