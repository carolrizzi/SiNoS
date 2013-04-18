package br.ufes.inf.lprm.sinos.common.rsv;

import java.rmi.Remote;
import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.situation.SituationType;

public interface SituationHolder extends Remote{

	public SituationType getSituation() throws RemoteException;
	
	public NotificationType getNotificationType () throws RemoteException;
}
