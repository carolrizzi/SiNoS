package br.ufes.inf.lprm.sinos.publisher.handler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;

public interface PublisherRequestHandler extends Remote{

	public static final String BIND_NAME = "SituationChannelPublisher";

	public void connect (String channelId, PublisherCallback callback, String publisherId) throws RemoteException;

	public void disconnect (PublisherCallback publisher, String channelId) throws RemoteException;

	public void publish (String channelId, SituationHolder situation) throws RemoteException;

	public ArrayList<String> getChannels () throws RemoteException;
	
	public boolean canCreateChannel () throws RemoteException;
	
}
