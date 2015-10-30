package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import spread.BasicMessageListener;
import spread.MembershipInfo;
import spread.MessageFactory;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class Client {

	/**
	 * Constructor that connnect to the server and join the specified group. Then it will wait unti
	 * all the defined members has joined and last reponds to input commands either from a file if the 
	 * fileName is defined or from commands from commandline. 
	 * @param serverAddress
	 * @param accountName
	 * @param numberOfReplicas
	 * @param uniqueName
	 * @param fileName
	 */
	public Client( String serverAddress, String accountName, Integer numberOfReplicas, String uniqueName, String fileName){
		setBankAccount(accountName);
		menu.respondToInput("deposit 0.0");
		
		
		setBankAccount(accountName);
		setNumberOfReplicas(numberOfReplicas);
		
		if(!connectToSpreadServer(serverAddress, 4803, uniqueName)){
			System.out.println("Could not connect to the provided server address: " + serverAddress+", try again");
			System.exit(2);
		}
		if(!joinGroup(accountName)){
			System.out.println("Could not connect to the desired group: " +serverAddress+", please try again");
			System.exit(3);
		}
		
		waitForMembers();

		
		if(fileName != null){
//			
			System.out.println("ParseFile");
			parseFile(fileName);
			
		}else{
			menu.showMenu();
		}
	}
	
	Menu menu = new Menu();
	
	/**
	 * Final values to ensure correct method handling for input and when recieved by the listener 
	 */
	public static final String DEPOSIT = "DEPOSIT";
	public static final String ADDINTREST = "ADDINTREST";
	public static final String WITHDRAW = "WITHDRAW";
	public static final String GETBALANCE = "GETBALANCE";
	
	/**
	 * A bank account that should be replicated accross replicas
	 */
	BankAccount bankAccount;

	/**
	 * The number of replicas in our group
	 */
	Integer numberOfReplicas;

	/**
	 * Connection to a spread server
	 */
	SpreadConnection connection;

	/**
	 * The group of the replicas
	 */
	SpreadGroup group;
	
	MessageListener listener = new MessageListener(this);


	/**
	 * Connect to a spread server with no priority and no groupMembership
	 * 
	 * @param serverAdress
	 * @param port
	 * @param uniqueConnName
	 * @return
	 */
	public boolean connectToSpreadServer(String serverAdress, Integer port, String uniqueConnName) {
		
		try {
			connection = new SpreadConnection();
			connection.connect(InetAddress.getByName(serverAdress), port, uniqueConnName, false, true);
			connection.add(listener);
			return true;
		} catch (UnknownHostException | SpreadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Disconnecting the connection
	 * 
	 * @return true/false if the connection was successful/unsuccessful
	 */
	public boolean disconnect() {
		try {
			group.leave();
			connection.remove(listener);
			connection.disconnect();
			return true;
		} catch (SpreadException e) {
			System.out.println("Disconnection failed");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Tries to join a group, the connection has to be established before this
	 * can be run
	 * 
	 * @param groupName
	 *            of the group
	 * @return true/false if the join was successfull/unsuccessful
	 */
	public boolean joinGroup(String groupName) {
		try {
			this.group = new SpreadGroup();
			group.join(connection, groupName);
			return true;
		} catch (SpreadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Tries to leave the connected group
	 * 
	 * @return true if it is successful
	 */
	public boolean leaveGroup() {
		try {
			this.group.leave();
			return true;
		} catch (SpreadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Setters and getters for the Client class attributes
	 * @param bankAccountName
	 */
	//*************BEGIN GETTERS AND SETTERS*************
	public void setBankAccount(String bankAccountName){
		bankAccount = new BankAccount(bankAccountName);
	}
	public BankAccount getBankAccount(){
		return this.bankAccount;
	}
	public void setNumberOfReplicas(Integer numberReplicas){
		this.numberOfReplicas = numberReplicas;
	}
	public Integer getNumberOfReplicas(){
		return this.numberOfReplicas;
	}
	//**************END GETTERS AND SETTERS*************
	
	/**
	 * Wait for all the members to join the group is called by the MessageListener if a user
	 *  leave the group
	 * @throws InterruptedException
	 */
	public void waitForMembers(){
		synchronized(listener){
			try {
				System.out.println(
						"******************************************************\n"
						+"*** Waiting for all the members to join the group ***\n"
						+"*****************************************************");
				listener.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}



	/**
	 * If the connection and the group is ok, the client will try to multicast a
	 * realiable message to all the users in the group
	 * 
	 * @param data
	 *            to send
	 * @return true/false if the message was delivered successful
	 */
	public boolean sendMessage(byte[] data) {

		if (connection == null || group == null) {
			System.out.println("The client is not correctly connected or has not joined a group");
			return false;
		}
		//Another way to recieve a message is by adding listeners
		SpreadMessage message = new SpreadMessage();
		message.setData(data);
		message.addGroup(group);
		message.setAgreed();
		try {
			connection.multicast(message);
			return true;
		} catch (SpreadException e) {
			e.printStackTrace();
			return false;
		}

	}
	/**
	 * Make the client sleep for x number of seconds
	 * @param duration 
	 * @throws InterruptedException
	 */
	public void sleep(Integer duration) throws InterruptedException{
		Thread.sleep(duration*1000);
	}
	
	
	private void parseFile(String filename){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
			
			String nextLine = reader.readLine();
			while(nextLine != null){
				if(nextLine.length()>2){
					menu.respondToInput(nextLine);
				}
				nextLine = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 *Menu class that display a menu and responds to inputs 
	 * @author Ida Marie Frøseth
	 *
	 */
	class Menu{
		
		public void showMenu(){
			System.out.printf(
					  "*****************************\n"
					+ "*           WELCOME         *\n"
					+ "*  REPLICATED BANK ACCOUNT  *\n"
					+ "*****************************\n");
			printCommands();
			
			Scanner scin = new Scanner(System.in);

			while(true){
				respondToInput(scin.nextLine());
			}
		}
		public void printCommands(){
			
				System.out.printf(
						"Supported commands: \n"
						+ "$ balance \n"
						+ "$ deposit <amount> \n"
						+ "$ withdraw <amount> \n"
						+ "$ addinterest <precent> \n"
						+ "$ sleep <duration>\n"
						+ "$ exit\n");
		
		}

		/**
		 * This method responds to inputs either coming from the command line or the fileparser. 
		 * If the input is a supported feature the method will generate a Agreed message and send
		 * it to the other member of the group.
		 * @param input
		 */
		public void respondToInput(String input){
			System.out.println("");
			String[] inputCmd = input.toLowerCase().split("\\s+");
			if(inputCmd.length==0){
				System.out.println("Bad command try:");
				printCommands();
				return;
			}
			System.out.println(inputCmd[0]);
			switch (inputCmd[0]){
				case "balance":
					System.out.println("The balance is: " + bankAccount.getBalance());	
					break;
					
				case "deposit":
					if(inputCmd.length!=2){
						System.out.println("Please use: balance <amount>");
						break;
					}
					sendMessage((Client.DEPOSIT + ":" + inputCmd[1]).getBytes());
					System.out.println("Deposing "+ inputCmd[1]);
					break;
					
				case "withdraw":
					if(inputCmd.length!=2){
						System.out.println("Please use: withdraw <amount>");
						break;
					}
					sendMessage((Client.WITHDRAW + ":" + inputCmd[1]).getBytes());
					System.out.println("Withdrawing "+ inputCmd[1]);
					break;
					
				case "addinterest":
					if(inputCmd.length!=2){
						System.out.println("Please use: addinterest <percent>");
						break;
					}
					sendMessage((Client.ADDINTREST + ":" + inputCmd[1]).getBytes());
					System.out.println("Adding interest "+ inputCmd[1]);
					break;	
					
				case "sleep":
					if(inputCmd.length!=2){
						System.out.println("Please use: sleep <duration>");
						break;
					}
					System.out.println("Sleeping for "+ inputCmd[1]);
					try {
						sleep(Integer.parseInt(inputCmd[1]));
					} catch (NumberFormatException | InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;	
					
				case "exit":
					System.out.println("System exiting");
					disconnect();				
					System.exit(1);
					break;
					
				default:
					System.out.println(inputCmd[0] + " is not a supported command");
					printCommands();
					break;
			}
				
		}
	}
	public static void main(String[] args) {
		//1 connect to the spread server
//		args = new String[]{"172.0.0.1", "NewGroup", "1"};
		if(args.length<3){
			System.out.printf("Wrong number of arguments, correct use: \n"
					+ "$java model.Client <server address> <account name> <number of replicas> <clientName> [file name]");
			System.exit(0);
		}
		Client client;
		if (args.length == 4) {
			client = new Client( args[0], args[1], Integer.parseInt(args[2]), args[3], null );
			System.out.println("ARGS WAS 5");

		} else if(args.length == 5) {
			client = new Client( args[0], args[1],Integer.parseInt(args[2]), args[3], args[4] );
			System.out.println("ARGS WAS 5");
			// parsefile
		}

	}
}
