package br.ufes.inf.lprm.sinos.common.rsv;

import java.rmi.Remote;
import java.rmi.RemoteException;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;



public interface Callback extends Remote{
	
	public void disconnect (final DisconnectionReason reason) throws RemoteException;
	
	public String getId () throws RemoteException;

}