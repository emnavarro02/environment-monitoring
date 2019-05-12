package com.ipleiria.mcs.datacollector.bridge.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
	ServerSocket m_ServerSocket;
	private volatile boolean started;
	
	public void start() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				started = true;
				
				try {
					m_ServerSocket = new ServerSocket(4444);
				} catch (IOException e) {
					System.out.println("Could not create a server socket at 4444. Exiting...");
					System.exit(-1);
				}
				System.out.println("Listening for clients on 4444");
				int id = 0;
				
				while (started) {
					try {
						
						Socket clientSocket = m_ServerSocket.accept();
						ClientServiceThread cliThread = new ClientServiceThread(clientSocket, id);
						cliThread.start();
					} catch (IOException ioe) {
						System.out.println("\"Exception encountered on accept. Ignoring. Stack Trace: ");
						ioe.printStackTrace();
					}
				}
				
			}
		}).start();
	}
	
	public void stop() {
		try {

			m_ServerSocket.close();
			started = false;
		} catch (IOException e) {
			System.out.println("Something went wrong while closing connections. Stack Trace: ");
			e.printStackTrace();
		}
		
		
	}

}
