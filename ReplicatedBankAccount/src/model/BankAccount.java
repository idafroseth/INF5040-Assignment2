package model;

public class BankAccount {
	/**
	 * Name of the account
	 */
	String accountName;
	
	/**
	 * Current balance of the account 
	 */
	double balance;
	
	public BankAccount(String accountName){
		this.accountName = accountName;
	}
	/**
	 * 
	 * @return the name of the account
	 */
	public String getAccountName(){
		return this.accountName;
	}
	/**
	 * configure the account Name
	 * @param name
	 */
	public void setAccountName(String name){
		this.accountName = name;
	}
	/**
	 * 
	 * @return the account balance
	 */
	public double getBalance(){
		return this.balance;
	}
	/**
	 * Change the balance to a new value
	 * @param newBalance
	 */
	public void setBalance(double newBalance){
		this.balance = newBalance;
	}
	
	public void addIntrest(double precent){
		this.balance = this.balance*(1+precent/100);
	}
	
	public void withdraw(double amount){
		this.balance -= amount;
	}
}
