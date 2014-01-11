package br.ufes.inf.lprm.sinos.channel.handler;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.channel.ChannelManager;
import br.ufes.inf.lprm.sinos.common.rsv.SituationHolder;
import br.ufes.inf.lprm.sinos.publisher.callback.PublisherCallback;
import br.ufes.inf.lprm.sinos.publisher.handler.PublisherRequestHandler;

public class PublisherRequestHandlerImpl extends CommonRequestHandler implements PublisherRequestHandler{
	
	public boolean ALLOW = true;
	
	public PublisherRequestHandlerImpl() {
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("sinos.properties"));
		} catch (SecurityException | IOException e) {
			System.err.println("[ERROR] Could not start Logger.");
			e.printStackTrace();
		}
	}
	
	@Override
	public void connect(String channelId, PublisherCallback callback, String publisherId) throws RemoteException {
		if(channelId == null || channelId.isEmpty() || channelId.split("&%&").length < 2){
			throw new RemoteException("Invalid channel id.");
		}
		String name = channelId.split("&%&")[0];
		
		ChannelManager channel; 
		synchronized (channels) {
			channel = channels.get(channelId);
			if(channel == null){
				if(ALLOW){
					channel = new ChannelManager(channelId, name);
					channels.put(channelId, channel);
					Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Channel " + name + " has been created.");
				}else{
					throw new RemoteException ("Channel " + name + " does not exists and cannot be created by publishers. Contact service's managers for more information.");
				}
			}
		}
		
		if(channel != null){
			channel.connectPublisher(callback, publisherId);
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Publisher " + publisherId + " is connected to channel " + name + ".");
			return;
		}
		
	}

	@Override
	public void disconnect(PublisherCallback publisher, String channelId) throws RemoteException {
		if(channelId == null || channelId == ""){
			return;
		}
		
		ChannelManager channel = channels.get(channelId);
		if(channel == null){
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Event channel not found: " + channelId);
			return;
		}
		
		channel.disconnectPublisher(publisher, null);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Publisher disconnected from channel " + channel.getChannelName() + ".");

	}
	
	@Override
	public void publish(String channelId, SituationHolder situation) throws RemoteException{
		if(channelId == null || channelId == ""){
			return;
		}
		
		ChannelManager channel = channels.get(channelId);
		if(channel == null) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Situation could not be published. Event channel not found: " + channelId);
			return;
		}
		
		channel.publish(situation.getNotificationType(), situation);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Situation published. Channel: " + channel.getChannelName());
	}

	@Override
	public ArrayList<String> getChannels() throws RemoteException {
		return CommonRequestHandler.getChannelsList();
	}

	@Override
	public boolean canCreateChannel() throws RemoteException {
		return ALLOW;
	}
}
