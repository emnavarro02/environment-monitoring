package com.ipleiria.mcs.datacollector.bridge.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
	ServerSocket m_ServerSocket;
	private volatile boolean started;
	
	int port = 9876;
	
	public void start() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				started = true;
				
				try {
					m_ServerSocket = new ServerSocket(port);
				} catch (IOException e) {
					System.out.println("Could not create a server socket at " + port + ". Exiting...");
					System.exit(-1);
				}
				System.out.println("Listening for clients on " + port + ".");
				int id = 0;
				
				while (started) {
					try {
						
						Socket clientSocket = m_ServerSocket.accept();
						ClientServiceThread cliThread = new ClientServiceThread(clientSocket, id);
						cliThread.start();

					} catch (IOException ioe) {
						System.out.println("\"Client has been disconnected. Ignoring...");
						// ioe.printStackTrace();
					}
				}
				
			}
		}).start();
	}
	
	public void stop() {
		try {
			started = false;
			m_ServerSocket.close();
			
		} catch (IOException e) {
			System.out.println("Something went wrong while closing connections: ");
			System.out.println(e.getMessage());
			// e.printStackTrace();
		}
		
		
	}

}
