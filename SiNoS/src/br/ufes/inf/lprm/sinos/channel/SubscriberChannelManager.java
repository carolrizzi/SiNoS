package br.ufes.inf.lprm.sinos.channel;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
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

//	private ArrayList<SubscriberCallback> subscribers = new ArrayList<>();
	private HashMap<SubscriberCallback, String> subscribers = new HashMap<>();

	public synchronized void subscribe (SubscriberCallback subscriber, String subscriberId) throws RemoteException {
		String id = subscriberId;
		if(id == null || id.isEmpty()){
			id = (UUID.randomUUID()).toString();
		}
//		subscribers.add(subscriber);
		subscribers.put(subscriber, id); //TODO: I think I could use a synchronized block here, instead of a synchronized method.

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "New Subscription: " + id);
	}
	
	public synchronized void unsubscribe (SubscriberCallback subscriber) throws RemoteException {
		String id = subscribers.remove(subscriber);
		if(id == null) {
			throw new RemoteException("Could not disconnect subscriber, because subscriber does not exist.");
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Subscriber disconnected: " + id);
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
		ExecutorService executor = Executors.newFixedThreadPool(size); //TODO: is this the better method to use here?
		
		for(final SubscriberCallback subscriber : subscribers.keySet()){
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
				//TODO: I could make it better, like having an array list for each notification type
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not notify subscriber. Reason: Could not check subscriber's notification type.", e);
			}
		}
		
		// TODO: I think I could make a dedicated class for this task, like a job that keeps running in parallel with the main program
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
	
	public synchronized void disconnectAllSubscribers (DisconnectionReason reason) {
		if(reason == null){
			reason = DisconnectionReason.UNKNOWN;
		}
		for(Entry<SubscriberCallback, String> subscriber : subscribers.entrySet()){
			sendDisconnectionNotification(subscriber.getKey(), subscriber.getValue(),  reason);
		}
		subscribers = new HashMap<>();
	}
	
	public void listSubscribers () {
		if(subscribers.isEmpty()){
			System.out.println("There are currently no subscriptions for this channel.");
			return;
		}
		for(Entry<SubscriberCallback, String> subscriberId : subscribers.entrySet())
			try {
				System.out.println("- " + subscriberId.getValue() + " (" + subscriberId.getKey().getOperation().toString().toLowerCase() + ")");
			} catch (RemoteException e) {
				System.out.println("- " + subscriberId.getValue() + " (unkonwn due to remote exception issues)");
			}
	}
	
	private void sendDisconnectionNotification (final SubscriberCallback subscriber, final String id, final DisconnectionReason reason) {
		if (reason == null) return;
		new Thread () {
			public void run() {
				boolean success = false;
				for(int i = 0; i < CommonRequestHandler.attempts; i++){
					try{
						subscriber.disconnect(reason);
						break;
					}catch (Exception e) {
						Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Attempt " + (i + 1) + " of reaching subscriber " + id + ".");
					}
				}
				if(!success){
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not notify disconnection to subscriber " + id + ", because subscriber is unreacheable");
				}
			}
		}.start();
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
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Attempt " + i + " of calling subscriber.", e);
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not contact subscriber. Removing it from subscriptions.");
		return subscriber;
	}
	
}