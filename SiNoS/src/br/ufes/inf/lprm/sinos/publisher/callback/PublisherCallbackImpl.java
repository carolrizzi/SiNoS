package br.ufes.inf.lprm.sinos.publisher.callback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.publisher.SituationChannel;

public class PublisherCallbackImpl extends UnicastRemoteObject implements PublisherCallback{

	private static final long serialVersionUID = 1L;
	private SituationChannel channel;
	private String publisherId = null;
	
	public PublisherCallbackImpl(SituationChannel channel) throws RemoteException {
		this.channel = channel;
		this.publisherId = channel.getId();
	}
	
	@Override
	public void disconnect(final DisconnectionReason reason) throws RemoteException {
		new Thread () {
			public void run () {
				channel.onDisconnection(reason);
			}
		}.start();
	}

	@Override
	public boolean checkConnection() throws RemoteException {
		return true;
	}

	@Override
	public String getId() throws RemoteException {
		return this.publisherId;
	}

}
