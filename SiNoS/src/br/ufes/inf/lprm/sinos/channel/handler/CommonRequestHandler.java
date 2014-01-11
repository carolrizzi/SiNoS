package br.ufes.inf.lprm.sinos.channel.handler;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.SiNoS;
import br.ufes.inf.lprm.sinos.channel.ChannelManager;
import br.ufes.inf.lprm.sinos.common.DisconnectionReason;
import br.ufes.inf.lprm.sinos.common.rsv.Callback;

public class CommonRequestHandler {

	protected static ConcurrentHashMap<String, ChannelManager> channels = new ConcurrentHashMap<>();
	
	public static InitProperties properties = new InitProperties();
	public static long attempts = Long.parseLong(properties.getProperty("attempts", "10"));
	public static long timeout = Long.parseLong(properties.getProperty("timeout", "60"));
//	public static long channelInactivityTimeout = Long.parseLong(properties.getProperty("channel-inactivity-timeout", "60"));
	
	public static ArrayList<String> getChannelsList () {
		ArrayList<String> channelsId = new ArrayList<>();
		for (String channelId : channels.keySet()){
			channelsId.add(channelId);
		}
		return channelsId;
	}
	
	public static void listPublishers () {
		if(channels.isEmpty()){
			System.out.println("There are currently no publishers registered in this service, because there are no open channels.");
			return;
		}
		for(ChannelManager channel : channels.values()){
			channel.listPublishers();
		}
	}
	
	public static void listPublishers (String channelId) {
		if(channels.isEmpty()){
			System.out.println("There are currently no publishers registered in this service, because there are no open channels.");
			return;
		}
		ChannelManager channel = channels.get(channelId);
		if(channel == null){
			System.out.println("Channel not found: " + channelId);
			return;
		}
		channel.listPublishers();
	}
	
	public static void listSubscribers () {
		if(channels.isEmpty()){
			System.out.println("There are currently no subscriptions registered in this service, because there are no open channels.");
			return;
		}
		for(ChannelManager channel : channels.values()){
			channel.listSubscribers();
		}
	}
	
	public static void listSubscribers (String channelId) {
		if(channels.isEmpty()){
			System.out.println("There are currently no subscriptions registered in this service, because there are no open channels.");
			return;
		}
		ChannelManager channel = channels.get(channelId);
		if(channel == null){
			System.err.println("Channel not found: " + channelId);
			return;
		}
		channel.listSubscribers();
	}
	
	public static void listChannels (boolean withId) {
		if(channels.isEmpty()){
			System.out.println("There are currently no open channels in this service.");
			return;
		}
		if(withId){
			System.out.println("List of active channels [name (id)]:");
			for(ChannelManager channel : channels.values()){
				System.out.println("- " + channel.getChannelName() + " (" + channel.getChannelId() + ")");
			}
		}else{
			System.out.println("List of active channels:");
			for(ChannelManager channel : channels.values()){
				System.out.println("- " + channel.getChannelName());
			}
		}
	}
	
	public static void closeChannel (String channelId) {
		ChannelManager channel = channels.get(channelId);
		if(channel == null){
			System.out.println("Channel not found: " + channelId);
			return;
		}
		channel.disconnectChannel(DisconnectionReason.CHANNEL_OFF);
		channels.remove(channelId);
		System.out.println("Channel " + channel.getChannelName() + " has been closed.");
	}
	
	public static void closeAllChannels (DisconnectionReason reason) {
		for(ChannelManager channel : channels.values()){
			channel.disconnectChannel(reason);
		}
		channels = new ConcurrentHashMap<>();
		System.out.println("All channels closed.");
	}
	
	public static class InitProperties {
		private static Properties prop;
		
		public InitProperties() {
			try{
				prop = new Properties();
				prop.load(new FileInputStream("sinos.properties"));
			}catch (Exception e) {
				Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Could not read properties from sinos.properties. Assigning default values.", e);
			}
		}
		
		public String getProperty (String property, String defaulValue){
			try {
				String result = prop.getProperty(property, defaulValue);
				Logger.getLogger(SiNoS.class.getName()).log(Level.INFO, result + " has been assigned to property '" + property + "'.");
				return result;
			} catch (Exception e) {
				Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Could not read property '" + property + "' from sinos.properties. Assigning default value to it (" + defaulValue + ").", e);
				return defaulValue;
			}
		}
	}

}


