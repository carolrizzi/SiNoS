package br.ufes.inf.lprm.sinos.publisher.callback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.situation.SituationType;


public class SituationHolderImpl extends UnicastRemoteObject implements SituationHolder{
	private static final long serialVersionUID = 1L;

	private SituationType situation;
	private NotificationType operation;
	
	public SituationHolderImpl(SituationType situation) throws RemoteException {
		this.situation = situation;
		if(situation.isActive()) operation = NotificationType.ACTIVATION;
		else operation = NotificationType.DEACTIVATION;
	}

	@Override
	public SituationType getSituation() {
		return situation;
	}
	
	@Override
	public NotificationType getNotificationType() throws RemoteException {
		return operation;
	}
	
}
