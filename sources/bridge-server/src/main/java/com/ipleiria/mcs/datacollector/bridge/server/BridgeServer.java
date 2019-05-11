package com.ipleiria.mcs.datacollector.bridge.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


public class BridgeServer implements BundleActivator
{	
	private static final String KEY_BLUETOOTH   = "bluetooth";
	private static final String KEY_DATA        = "data";
	private static final String KEY_HEIGHT      = "height";
	private static final String KEY_TEMPERATURE = "temperature";
	private static final String KEY_BATTERY     = "battery";
	private static final String KEY_PRESSURE    = "pressure";
	private static final String KEY_ID          = "id";
	private static final String KEY_LOCATION    = "location";
	private static final String KEY_SENSORS     = "sensors";
	
	private static final String SENSORS_FILE  = "sensors.json";
	
	private static final String TOPIC_BASE                = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_TEMPERATURE_HUMDITY = TOPIC_BASE + "TempHum";
	private static final String TOPIC_NEW_SENSORS         = TOPIC_BASE + "sensors";

	static ServerSocket serverSocket;
	static boolean done = false;
	
	
	@Override
	public void start(BundleContext context) throws Exception {
		main();
	}
	
	public void main() {

		try {
			Scanner consoleIn = new Scanner(System.in);
			//TODO: check for empty ports.
			System.out.print("Listen port: ");
			int port = Integer.parseInt(consoleIn.nextLine());
			
			System.out.println("Loading sensors from file " + SENSORS_FILE);
			String list = readFile(SENSORS_FILE);
			
			serverSocket = new ServerSocket(port);
			
			System.out.println("Waiting for connection...");
			Socket socket = serverSocket.accept();
			
			Scanner socketIn = new Scanner(socket.getInputStream()); 
			PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
			
			String clientName = socketIn.nextLine();
			System.out.println("Client " + clientName + " connected. Sending list...");
			socketOut.println(list);
			
			do
			{
				// Get a message from the client; if it's empty; we're done
				String message = socketIn.nextLine();
				if (message.length() == 0)
				{
					System.out.println("Client ended conversation");
					done = true;
				}
				
				// if it is not empty, try to extract it content
				else
				{
					processMessage(message);
				}
				
			} while (!done);
			
			System.out.println("Closing connection");
			consoleIn.close();
			socketIn.close();
			socketOut.close();
			serverSocket.close();
			socket.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		serverSocket.close();		
	}
	
	private void sendData(BundleContext context, Map<String, ?> data) {
		ServiceReference eventAdminRef = context.getServiceReference(EventAdmin.class.getName());
		if (eventAdminRef != null) {
			EventAdmin eventAdmin = (EventAdmin) context.getService(eventAdminRef);
			
			Event newEvent = new Event(TOPIC_TEMPERATURE_HUMDITY, data);
			System.out.println("Ready to send data: " + newEvent);
			eventAdmin.sendEvent(newEvent);
		}
	}
	
	private static void processMessage(String message) {
		System.out.println("JSON ARRAY");
		//JSONArray jsonArray = new  for(int i =0; i< jsonArray.length(); i++){
//          if(jsonArray.get(i) instanceof JSONObject){
//          JSONObject jsnObj = (JSONObject)jsonArray.get(i);
//          String finalValue = (String)jsnObj.get("id");
//      }
//  }JSONArray(message);
//        for(int i =0; i< jsonArray.length(); i++){
//            if(jsonArray.get(i) instanceof JSONObject){
//                JSONObject jsnObj = (JSONObject)jsonArray.get(i);
//                String finalValue = (String)jsnObj.get("id");
//            }
//        }
	}
	
	private static String readFile(String fileName) {
		StringBuilder sb = new StringBuilder();
		try
		{
			FileReader file = new FileReader(fileName);
			BufferedReader buffer = new BufferedReader(file);
			
			String line = buffer.readLine();
			
			while(line != null)
			{
				sb.append(line).append('\n');
				line = buffer.readLine();
			}
			buffer.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		String jsonAsString = sb.toString();
		return jsonAsString;
	}
	
}
