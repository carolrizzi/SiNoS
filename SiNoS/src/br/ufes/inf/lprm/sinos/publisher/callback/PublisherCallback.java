package br.ufes.inf.lprm.sinos.publisher.callback;

import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.common.rsv.Callback;

public interface PublisherCallback extends Callback {
	
	public boolean checkConnection () throws RemoteException; 
	
}
