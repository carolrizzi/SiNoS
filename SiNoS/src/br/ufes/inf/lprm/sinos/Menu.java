package br.ufes.inf.lprm.sinos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.ufes.inf.lprm.sinos.channel.handler.CommonRequestHandler;

public class Menu {

	private static SiNoS manager;
	
	public Menu(SiNoS manager) {
		Menu.manager = manager;
		
		System.out.println("Type 'help' to list commands.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.print("> ");
			try {
				String [] cmd = br.readLine().split(" ");
				switch (cmd[0].toLowerCase()) {
				case "help":
					System.out.println("\nstop:\tDisconnects all publishers and subscribers, closes all channels and stops the service.");
					Channel.help();
					Publisher.help();
					Subscriber.help();
					break;
					
				case "stop":
					manager.stop();
					break;

				case Channel.CMD_NAME:
//					if (validate(cmd, 2, Channel.CMD_NAME))
						Channel.menu(cmd);
					break;

				case Publisher.CMD_NAME:
//					if (validate(cmd, 2, Publisher.CMD_NAME))
						Publisher.menu(cmd);
					break;

				case Subscriber.CMD_NAME:
//					if (validate(cmd, 2, Subscriber.CMD_NAME))
						Subscriber.menu(cmd);
					break;
					
				default:
					System.err.println("Invalid command: " + cmd[0]);
					break;
				}
				
			} catch (IOException e) {
				Logger.getLogger(SiNoS.class.getName()).log(Level.WARNING, "Could not read command.", e);
			}
		}
	}
	
//	public static boolean validate (String [] cmd, int size, String cmdName) {
//		if(cmd.length < size) {
//			System.err.println("Invalid number of parameters for command '" + cmdName + "'.");
//			return false;
//		}
//		return true;
//	}
	
	public static boolean validateOption (String cmdName, String option) {
		if(!option.startsWith("-") || option.length() < 2){
			invalidOptionMessage(cmdName, option);
			return false;
		}
		return true;
	}
	
	public static String validateOptionParameter (String option, int optionIndex, String errorMessage) {
		if(option.length() <= optionIndex+1){
			System.err.println(errorMessage);
			return null;
		}
		return option.substring(optionIndex+1);
	}
	
	public static void invalidOptionMessage (String cmdName, String option) {
		System.err.println("Invalid option for command " + cmdName + ": " + option + "\nType 'help' for more information.");
	}
	
	public static String getSpace (int enter, int tab, int space) {
		String str = "";
		for (int i = 0; i < enter; i++){
			str += "\n";
		}
		for (int i = 0; i < tab; i++){
			str += "\t";
		}
		for (int i = 0; i < space; i++){
			str += "   ";
		}
		return str;
	}

	private static class Channel {
		public static final String CMD_NAME = "chn";
		private static final String OPT1 = "c";
		private static final String OPT2 = "s";
		private static final String OPT3 = "p";
		
		public static void help () {
			System.out.println("\n" + CMD_NAME + ":\tControls the existing channels." + Menu.getSpace(1, 1, 0) + "If no option is specified, this command lists the existing channels." + Menu.getSpace(1, 1, 0) + "Usage: " + CMD_NAME + " [option]" + Menu.getSpace(1, 1, 0) + "Options:");
			
			System.out.println(Menu.getSpace(0, 1, 1) + "-" + OPT1 + ":\tCloses the channel with the provided id" + Menu.getSpace(1, 2, 0) + "Usage: " + CMD_NAME + " -" + OPT1 + "<channel_id>" + Menu.getSpace(1, 2, 0) + "Example: " + CMD_NAME + " -" + OPT1 + "myChannelId");
			System.out.println(Menu.getSpace(0, 1, 1) + "-" + OPT2 + ":\tShows the current permission status for publishers to create new channels." + Menu.getSpace(1, 2, 0) + "Usage: " + CMD_NAME + " -" + OPT2);
			System.out.println(Menu.getSpace(0, 1, 1) + "-" + OPT3 + ":\tChanges the current permission status for publishers to create new channels." + Menu.getSpace(1, 2, 0) + "Usage: " + CMD_NAME + " -" + OPT3);
		}
		
		public static void menu (String [] cmd) {
			if(cmd.length <= 1){
				CommonRequestHandler.listChannels(true);
				return;
			}
			for (int count = 1; count < cmd.length; count++) {
				String option = cmd[count];
				if (!validateOption(CMD_NAME, option)) return;
				for(int i = 1; i < option.length(); i++){
					switch (option.charAt(i)) {
						case 'c': // close channel
							String parameter = validateOptionParameter(option, i, "Please, provide the id of the channel to be closed.");
							if(parameter == null) return;
							CommonRequestHandler.closeChannel(parameter);
							i = option.length();
							break;
							
						case 's': // show status
							System.out.println("Channel creation is currently " + (manager.creationStatus() ? "enabled" : "disabled") + " for publishers.");
							break;
							
						case 'p': // change creation permission
							manager.changeChannelCreationPermission();
							System.out.println("Channel creation has been " + (manager.creationStatus() ? "enabled" : "disabled") + " for publishers.");
							break;
							
						default:
							invalidOptionMessage(CMD_NAME, option);
					}
				}
				
			}
		}
		
	}
	
	private static class Publisher {
		public static final String CMD_NAME = "pub";
		private static final String OPT1 = "f";
		
		public static void help () {
			System.out.println("\n" + CMD_NAME + ":\tcontrol subscribers." + Menu.getSpace(1, 1, 0) + "If no option is specified, this command lists all registered publishers." + Menu.getSpace(1, 1, 0) + "Usage: " + CMD_NAME + " [option]" + Menu.getSpace(1, 1, 0) + "Options:");
			
			System.out.println(Menu.getSpace(0, 1, 1) + "-" + OPT1 + ":\tFilters the list of publishers by channel." + Menu.getSpace(1, 2, 0) + "Usage: " + CMD_NAME + " -" + OPT1 + "<channel_id>" + Menu.getSpace(1, 2, 0) + "Example: " + CMD_NAME + " -" + OPT1 + "myChannelId");
		}
		
		public static void menu (String [] cmd) {
			if(cmd.length <= 1){
				 CommonRequestHandler.listPublishers();
				 return;
			}
			for (int count = 1; count < cmd.length; count++) {
				String option = cmd[count];
				validateOption(CMD_NAME, option);
				for(int i = 1; i < option.length(); i++){
					switch (option.charAt(i)) {
						case 'f': // filter by channel
							String parameter = validateOptionParameter(option, i, "Please, provide the id of the channel to use as filter.");
							if(parameter == null) return;
							CommonRequestHandler.listPublishers(parameter);
							i = option.length();
							break;
							
						default:
							invalidOptionMessage(CMD_NAME, option);
					}
				}
				
			}
		}
	}
	
	private static class Subscriber {
		public static final String CMD_NAME = "sub";
		private static final String OPT1 = "f";
		
		public static void help () {
			System.out.println("\n" + CMD_NAME + ":\tcontrol subscribers." + Menu.getSpace(1, 1, 0) + "If no option is specified, this command lists all registered subscribers." + Menu.getSpace(1, 1, 0) + "Usage: " + CMD_NAME + " [option]" + Menu.getSpace(1, 1, 0) + "Options:");

			System.out.println(Menu.getSpace(0, 1, 1) + "-" + OPT1 + ":\tFilters the list of subscribers by channel." + Menu.getSpace(1, 2, 0) + "Usage: " + CMD_NAME + " -" + OPT1 + "<channel_id>" + Menu.getSpace(1, 2, 0) + "Example: " + CMD_NAME + " -" + OPT1 + "myChannelId");
		}
		
		public static void menu (String [] cmd) {
			if(cmd.length <= 1){
				 CommonRequestHandler.listSubscribers();
				 return;
			}
			for (int count = 1; count < cmd.length; count++) {
				String option = cmd[count];
				validateOption(CMD_NAME, option);
				for(int i = 1; i < option.length(); i++){
					switch (option.charAt(i)) {
						case 'f': // filter by channel
							String parameter = validateOptionParameter(option, i, "Please, provide the id of the channel to use as filter.");
							if(parameter == null) return;
							CommonRequestHandler.listSubscribers(parameter);
							i = option.length();
							break;
							
						default:
							invalidOptionMessage(CMD_NAME, option);
					}
				}
				
			}
		}
	}
	
}
