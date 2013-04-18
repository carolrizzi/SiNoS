package br.ufes.inf.lprm.sinos.channel.handler;

import java.io.FileInputStream;
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
	public static long channelInactivityTimeout = Long.parseLong(properties.getProperty("channel-inactivity-timeout", "60"));
	

	public static void disconnectAll (DisconnectionReason reason) {
		for(ChannelManager channel : channels.values()){
			channel.disconnect(reason);
		}
	}

	public static void disconnect (final Callback callback, final DisconnectionReason reason) {
		new Thread () {
			public void run() {
				boolean success = false;
				for(int i = 0; i < attempts; i++){
					try{
						callback.disconnect(reason);
						break;
					}catch (Exception e) {
						Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Attempt of reaching client: " + (i + 1));
					}
				}
				if(!success){
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not notify client about disconnection.");
				}
			}
		}.start();
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

	public static void closeChannel(String channelName) {
		channels.remove(channelName);
	}
}


