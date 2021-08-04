package appl;

import core.Server;
import java.util.Scanner;

/*
* Código que se comporta como servidor e cliente
* Existe um Broker por variável.
* (bem confusa a explicação do Joubert)
*/ 


public class Broker {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Broker();
	}
	
	public Broker(){		
		/*
		* @Var: port -> Valor lido da entrada que define qual porta o seridor irá se conectar
		* @Var: s -> Servidor criado na usando port
		* @Var: brokerThread -> Cria uma thread para o servidor s
				É Ativado com o comando start()
		*/
		Scanner reader = new Scanner(System.in);  // Reading from System.in
		System.out.print("Enter the Broker port number: ");
		int port = reader.nextInt(); // Scans the next token of the input as an int.
		
		
		Server s = new Server(port);
		ThreadWrapper brokerThread = new ThreadWrapper(s);
		brokerThread.start();
		
		System.out.print("Shutdown the broker (Y|N)?: ");
		String resp = reader.next(); 
		if (resp.equals("Y") || resp.equals("y")){
			System.out.println("Broker stopped...");
			s.stop();
			brokerThread.interrupt();
			
		}
		
		//once finished
		reader.close();
	}
	
	class ThreadWrapper extends Thread{
		/*
		* @Var: s -> Servidor usado na Thread
		*/
		Server s;
		public ThreadWrapper(Server s){
			/*
			 * @param: s -> Servidor que será usado na thread 
			 */
			this.s = s;
		}

		public void run(){
			/*
			 * Coloca a Thread em execução 
			 */
			s.begin();
		}
	}

}
