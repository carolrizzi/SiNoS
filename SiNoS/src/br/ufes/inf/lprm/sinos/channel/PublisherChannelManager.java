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

import br.ufes.inf.lprm.sinos.channel.handler.CommonRequestHandler;
import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;

public class PublisherChannelManager {

	private class PublisherWrapper {
		public int id;
		public PublisherCallback publisher;
		
		public PublisherWrapper(int id, PublisherCallback publisher) {
			this.id = id;
			this.publisher = publisher;
		}
	}
	private ArrayList<PublisherWrapper> publishers = new ArrayList<>();
	private PublisherWrapper original;
	private ExecutorService executor;
	private Future<Boolean> future;
	private ChannelManager myManager;
	
	public PublisherChannelManager(PublisherCallback publisher, String channelName, ChannelManager channelManager) throws RemoteException {
		this.myManager = channelManager;
		executor = Executors.newSingleThreadExecutor();
		original = new PublisherWrapper(publisher.getId(), publisher);
		start();
	}
	
	private void start () {
		new Thread(){
			public void run () {
				try {
					do{
						future = executor.submit(new CheckChannelActivity());
					}while(future.get());
				} catch (Exception e) {return;} //InterruptedException | ExecutionException
				executor.shutdownNow();
				myManager.unsubscribeAllAndCloseChannel(DisconnectionReason.PUBLISHERS_OFF);
			}
		}.start();
	}
	
	public synchronized void addPublisher (PublisherCallback publisher) throws RemoteException {
		publishers.add(new PublisherWrapper(publisher.getId(), publisher));
	}
	
	public synchronized void disconnect (DisconnectionReason reason){
		executor.shutdownNow();
		disconnectAssistants(reason);
		CommonRequestHandler.disconnect(original.publisher, reason);
	}
	
	public synchronized boolean closeChannel (int publisherId) {
		if(original.id == publisherId){
			disconnectAssistants(DisconnectionReason.CHANNEL_OFF);
			executor.shutdownNow();
			return true;
		}else{
			for(PublisherWrapper wrapper : publishers){
				if(wrapper.id == publisherId){
					publishers.remove(wrapper);
					break;
				}
			}
			return false;
		}
	}
	
//	public void restart () {
//		future.cancel(false);
//		start();
//	}
	
	private void disconnectAssistants (DisconnectionReason reason) {
		for(PublisherWrapper wrapper : publishers){
			CommonRequestHandler.disconnect(wrapper.publisher, reason);
		}
	}
	
	class CheckChannelActivity implements Callable<Boolean>{
		class Task{
			public PublisherWrapper wrapper;
			public Future<Boolean> future;
			public Task(PublisherWrapper wrapper, Future<Boolean> future) {
				this.wrapper = wrapper;
				this.future = future;
			}
		}
		
		@Override
		public Boolean call() throws Exception {
			try{Thread.sleep(CommonRequestHandler.channelInactivityTimeout * 1000);}catch(Exception e){}
			ExecutorService executor = Executors.newSingleThreadExecutor(); //newFixedThreadPool(publishers.size() + 1);
			ArrayList<Task> tasks = new ArrayList<>();
			
			tasks.add(new Task(original,  executor.submit(new CheckConnection(original.publisher))));
			for(PublisherWrapper publisher : publishers) {
				tasks.add(new Task(publisher, executor.submit(new CheckConnection(publisher.publisher))));
			}
			
			boolean isAnybodyThere = false;
			for(Task task : tasks){
				try {
					task.future.get(CommonRequestHandler.timeout, TimeUnit.SECONDS);
					isAnybodyThere = true;
				} catch (InterruptedException | ExecutionException e) {
				} catch (TimeoutException e) {
					synchronized (publishers) {
						publishers.remove(task.wrapper);
						//TODO: could be better
					}
				}
			}
			executor.shutdown();
			return isAnybodyThere;
		}
	}
}

class CheckConnection implements Callable<Boolean> {
	PublisherCallback publisher;
	
	public CheckConnection(PublisherCallback publisher) {
		this.publisher = publisher;
	}
	
	@Override
	public Boolean call() throws Exception {
		return publisher.checkConnection();
	}
}
