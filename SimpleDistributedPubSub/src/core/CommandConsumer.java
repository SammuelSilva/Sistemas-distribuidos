package core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.SortedSet;

import core.client.Client;
import utils.Tuple;

public class CommandConsumer<S extends Tuple<Socket, Message>> extends GenericConsumer<S>{
    private Integer uniqueLogId;

	private SortedSet<Message> log;
	private List<String> subscribers;

	private Address primary;
	private boolean isPrimary;

	private Address backup;

	public CommandConsumer(GenericResource<S> re, boolean isPrimary, Address address, SortedSet<Message> log, List<String> subscribers) {		
        super(re);
        this.uniqueLogId = 1;
        this.log = log;
        this.subscribers = subscribers;
        this.primary = new Address(null, 0);
        this.backup = new Address(null, 0);
        
        this.isPrimary = isPrimary;

        if(isPrimary) {
            backup.setIp(address.getIp());
            backup.setPort(address.getPort());
        } else {
            primary.setIp(address.getIp());
            primary.setPort(address.getPort());
        }
    }

    @Override
    protected void doSomething(S tuple) {
        Socket key = tuple.getKey();
        Message value = tuple.getValue();

        //System.out.println("CommandConsumer: " + value + " " + key + " " + !isPrimary + " " + value.getType());
        Message response = new MessageImpl();

        if(!isPrimary && value.getType().equals("recover")) {
            changePrimary(value);
        }
            
        if (!isPrimary) {
			System.out.println("[ Backup ] Sending the message from " +  value.getType()  + " to the Main Broker");
            try {
                System.out.println("[Backup] Ip and Port: " +  primary.getIp() + " " + primary.getPort());
                Client client = new Client(primary.getIp(), primary.getPort());
                response = client.sendReceive(value);
            } catch (IOException e) {
                System.out.println("[Backup] Not Able to Connect with the Main Broker. Changing to backup " + value.getType());
				changePrimary();
				System.out.println("[Backup] Current Log id: " + uniqueLogId);
				response = executeCommand(value);
            }

        }else{
            response = executeCommand(value);
        }

        try{
            ObjectOutputStream oos = new ObjectOutputStream(key.getOutputStream());
            oos.writeObject(response);
            oos.flush();
            oos.close();
            key.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private Message executeCommand(Message message) {
        Message response = new MessageImpl();

        if(message.getType().equals("recover")) {
            response = message;
            response.setType("recover_ack");
            return response;
        }

        if(!(message.getType().equals("notify"))) {
            message.setLogId(uniqueLogId);
        }

        //System.out.println("TYPE: " + message.getType());
        response = commands.get(message.getType()).execute(message, log, subscribers, backup);

        if(!(message.getType().equals("notify"))) {
            uniqueLogId = message.getLogId();
        }

        return response;
    }

    private void changePrimary(Message message) {
        //tenta executar verificando se o main esta up
        try {
            Client client = new Client(primary.getIp(), primary.getPort());
            client.sendReceive(message);
        }catch(IOException e) {
            changePrimary();
        }
    }

    private void changePrimary() {
        this.uniqueLogId = this.log.last().getLogId();
		this.isPrimary = true;
    }

    @Override
	public SortedSet<Message> getMessages() {
		return log;
	}	
}
