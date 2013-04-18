package br.ufes.inf.lprm.sinos.subscriber.callback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.subscriber.SituationListener;
import br.ufes.inf.lprm.situation.SituationType;

public class DeactivationCallback<T extends SituationType> extends UnicastRemoteObject implements SubscriberCallback{

	private static final long serialVersionUID = 1L;

	private SituationListener<T> handler;

	private boolean notifyDisconnection;
	
	public DeactivationCallback(SituationListener<T> handler, boolean notifyDisconnection) throws RemoteException {
		this.handler = handler;
		this.notifyDisconnection = notifyDisconnection;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void call(SituationHolder situation) throws RemoteException {
		handler.onSituationDeactivation((T) situation.getSituation());
	}

	@Override
	public String getEventChannel() throws RemoteException {
		return handler.getChannelName();
	}

	@Override
	public NotificationType getOperation() throws RemoteException {
		return NotificationType.DEACTIVATION;
	}

	@Override
	public void disconnect(final DisconnectionReason reason) throws RemoteException {
		if(notifyDisconnection){
			new Thread () {
				public void run () {
					handler.onDisconnection(reason);
				}
			}.start();
		}
	}

	@Override
	public int getId() throws RemoteException {
		return handler.hashCode();
	}

}
