package br.ufes.inf.lprm.sinos.channel.handler;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.channel.ChannelManager;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;
import br.ufes.inf.lprm.sinos.publisher.handler.PublisherRequestHandler;

public class PublisherRequestHandlerImpl extends CommonRequestHandler implements PublisherRequestHandler{
	
	public PublisherRequestHandlerImpl() {
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("sinos.properties"));
		} catch (SecurityException | IOException e) {
			System.err.println("[ERROR] Could not start Logger.");
			e.printStackTrace();
		}
	}
	
	@Override
	public void createChannel(String channelName, PublisherCallback callback) throws RemoteException {
		if(channelName == null || channelName == ""){
			return;
		}
		
		ChannelManager channel; 
		synchronized (channels) {
			channel = channels.get(channelName);
			if(channel == null){
				channels.put(channelName, new ChannelManager(channelName, callback));
			}
		}
		
//		ChannelManager channel = channels.putIfAbsent(channelName, new ChannelManager(channelName, callback));
		if(channel != null){
			channel.addPublisher(callback);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Publisher added to channel " + channelName + ".");
			return;
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Channel " + channelName + " created.");
	}

	@Override
	public void closeChannel(int publisherId, String channelName) throws RemoteException {
		if(channelName == null || channelName == ""){
			return;
		}
		
		ChannelManager channel = channels.get(channelName);
		if(channel == null){
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Event channel not found: " + channelName);
			return;
		}
		
		channel.closeChannel(publisherId);

	}
	
	@Override
	public void publishSituation(String channelName, SituationHolder situation) throws RemoteException{
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Publication requested. Channel: " + channelName);
		if(channelName == null || channelName == ""){
			return;
		}
		
		ChannelManager channel = channels.get(channelName);
		if(channel == null) {
			return;
		}
		
		channel.publish(situation.getNotificationType(), situation);
	}
}
