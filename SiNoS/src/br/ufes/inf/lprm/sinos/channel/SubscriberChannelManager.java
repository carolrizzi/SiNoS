package br.ufes.inf.lprm.sinos.channel;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.channel.handler.CommonRequestHandler;
import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.common.NotificationType;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.subscriber.callback.SubscriberCallback;

public class SubscriberChannelManager {

	private ArrayList<SubscriberCallback> subscribers = new ArrayList<>();

	public synchronized void subscribe (SubscriberCallback subscriber) {
		try {
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "New Subscription: " + subscriber.getId());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		subscribers.add(subscriber);
	}
	
	public synchronized void unsubscribe (SubscriberCallback subscriber) {
		boolean contains = subscribers.remove(subscriber);
		try {
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Subscription removed: " + subscriber.getId() + ". Contains: " + contains);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void publish (final NotificationType notificationType, final SituationHolder situation) {
		int size = subscribers.size(); 
		if(size <= 0) return;
		
		class Task {
			public SubscriberCallback subscriber;
			public Future<SubscriberCallback> future;
			public Task (SubscriberCallback subscriber, Future<SubscriberCallback> future) {
				this.subscriber = subscriber;
				this.future = future;
			}
		}
		
		ArrayList<Task> toRemove = new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(size);
		
		for(final SubscriberCallback subscriber : subscribers){
			try {
				final NotificationType subscriberOperation = subscriber.getOperation();
				new Thread () {
					public void run () {
						if(subscriberOperation.equals(notificationType) || subscriberOperation.equals(NotificationType.MIRRORING)){
							try {
								subscriber.call(situation);
							} catch (RemoteException e) {
								Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not notify subscriber.", e);
							}
						}
					}
				}.start();
			} catch (RemoteException e) {
				toRemove.add(new Task(subscriber, executor.submit(new CheckSubscriberConnection(subscriber))));
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not notify subscriber. Reason: Could not check subscriber's notification type.", e);
			}
		}
		
		for(Task task : toRemove){
			try {
				SubscriberCallback subscriber = task.future.get(CommonRequestHandler.timeout, TimeUnit.SECONDS);
				if(subscriber != null){
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not contact consumer. Removing it from subscriptions.");
					synchronized (subscribers) {
						subscribers.remove(subscriber);
					}
				}
			} catch (InterruptedException | ExecutionException e) {
			}catch (TimeoutException e) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not contact consumer. Removing it from subscriptions.");
				synchronized (subscribers) {
					subscribers.remove(task.subscriber);
				}
			}
		}
		executor.shutdown();
	}
	
	public synchronized void disconnect (DisconnectionReason reason) {
		for(SubscriberCallback subscriber : subscribers){
			CommonRequestHandler.disconnect(subscriber, reason);
		}
		subscribers = new ArrayList<>();
	}
}

class CheckSubscriberConnection implements Callable<SubscriberCallback>{

	private SubscriberCallback subscriber;

	public CheckSubscriberConnection(SubscriberCallback subscriber){
		this.subscriber = subscriber;
	}
	
	@Override
	public SubscriberCallback call() throws Exception {
		for(int i = 0; i < CommonRequestHandler.attempts; i++){
			try{
				subscriber.getOperation();
				return null;
			}catch (Exception e) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Attempt " + i + " of calling consumer.", e);
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not contact subscriber. Removing it from subscriptions.");
		return subscriber;
	}
	
}