package br.ufes.inf.lprm.sinos.subscriber.callback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.subscriber.SituationListener;
import br.ufes.inf.lprm.situation.SituationType;
import br.ufes.inf.lprm.situation.events.ActivateSituationEvent;

public class MirroringCallback<T extends SituationType> extends UnicastRemoteObject implements SubscriberCallback{

	private static final long serialVersionUID = 1L;

	private SituationListener<T> handler;
	private StatefulKnowledgeSession ksession;
	private HashMap<Integer, FactHandle> factHandles = new HashMap<Integer, FactHandle>();
	T previous = null;
	private boolean notifyDisconnection;
	private String subscriberId;
	
	public MirroringCallback(String subscriberId, SituationListener<T> handler, boolean notifyDisconnection) throws RemoteException {
		this.subscriberId = subscriberId;
		this.handler = handler;
		this.notifyDisconnection = notifyDisconnection;
		this.ksession = handler.getStatefulKnowledgeSession();
	}

	private FactHandle activateSituation (T sit) throws Exception {
		sit.setLocal(false);
		ksession.insert(sit.getActivation());
		FactHandle fh = ksession.insert(sit);
    	
    	return fh;
	}
	
	@SuppressWarnings("unchecked")
	private void deactivateSituation (FactHandle fh, T sit) {
		sit.setLocal(false);
		SituationType currentSituation = (T) ksession.getObject(fh);
		
		ActivateSituationEvent activation = currentSituation.getActivation();
		FactHandle activationFH = ksession.getFactHandle(activation);
		if(activationFH == null){
			return;
		}
		activation.setSituation(sit);
		sit.setActivation(activation);
		
		sit.setInactive();
		ksession.update(activationFH, activation);
		ksession.insert(sit.getDeactivation());
		ksession.update(fh, sit);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void call(SituationHolder situation) throws RemoteException {
		FactHandle factHandle;
		T sit = (T) situation.getSituation();
		
		try{
			if(sit.getDeactivation() == null){
				previous = sit;
				factHandle = activateSituation (sit);
				factHandles.put(sit.hashCode(), factHandle);
			}else{
				factHandle = factHandles.remove(sit.hashCode());
				if(factHandle != null) {
					deactivateSituation(factHandle, sit);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getChannelId() throws RemoteException {
		return handler.getChannelId();
	}

	@Override
	public NotificationType getOperation() throws RemoteException {
		return NotificationType.MIRRORING;
	}

	@Override
	public void disconnect(final DisconnectionReason reason) throws RemoteException {
		if(reason == null) return;
		if(notifyDisconnection){
			new Thread () {
				public void run () {
					handler.onDisconnection(reason);
				}
			}.start();
		}
	}

	@Override
	public String getId() throws RemoteException {
		return this.subscriberId;
	}


}
