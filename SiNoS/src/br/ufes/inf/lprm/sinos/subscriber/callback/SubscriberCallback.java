package br.ufes.inf.lprm.sinos.subscriber.callback;

import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.Callback;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;

public interface SubscriberCallback extends Callback{
	
	public void call (SituationHolder situation) throws RemoteException;
	
	public String getChannelId () throws RemoteException;
	
	public NotificationType getOperation () throws RemoteException;

}
