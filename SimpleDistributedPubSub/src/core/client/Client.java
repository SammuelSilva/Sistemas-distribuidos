package core.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import core.Message;

public class Client {
	
	private Socket s;
	
	public Client(String ip, int port){
		try{
			s = new Socket(ip, port);
		}catch (Exception e){
			System.out.println("Client cannot connect with " + ip + " on port: " + port);
			
		}
	}
	
	public Message sendReceive(Message msg){
		try{
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
			out.writeObject(msg);
			out.flush();
			
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
			Message response = (Message) in.readObject();
			
			in.close();
			out.close();			
			s.close();
			return response;
		}catch(Exception e){
			return null;
		}
	}

}
