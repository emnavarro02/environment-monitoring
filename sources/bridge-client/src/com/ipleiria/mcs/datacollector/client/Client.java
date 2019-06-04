package com.ipleiria.mcs.datacollector.client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothManager;

public class Client
{
	public static boolean done = false;
	
	public static void main(String[] args) throws IOException, InterruptedException, NumberFormatException
	{
		Scanner consoleIn = new Scanner(System.in);

		System.out.print("Server IP address (Enter to default [localhost]): ");
		String address = consoleIn.nextLine();
		if (address.isEmpty()) {
			address = "localhost";
		}
		
		System.out.println(address);
		
		System.out.print("Port number (Enter to default [4444]): ");
		
		int port = 4444; 
	 	String portNumber = consoleIn.nextLine();
	 	
	 	if(portNumber.isEmpty()) 
		{
			port = 4444;
		}
	 	else
	 	{ 
	 		try 
	 		{
	 			port = Integer.parseInt(portNumber);
	 		}
	 		catch (NumberFormatException e)
	 		{
	 			System.err.println("Port number invalid. Attempting to connect with default [4444].\n");
	 		}
	 	}
		
		System.out.println("Connecting");
		Socket socket = new Socket(address, port);
		System.out.println("Connected to port " + port + " at " + address);
		
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		
		
		System.out.println("Input and output streams established");
	
    	do
		{
			try 
			{	
				BluetoothManager manager = BluetoothManager.getBluetoothManager();
		        
		        List<BluetoothDevice> list = getBluetoothDevices();
		 
		        for (BluetoothDevice obj : list) {
		        	
		            // Devices that are broadcasting
		        	// try to get serviceData information, which is an Map with service profile and data like: {0000000-000-00000, @byte[]}
		        	Map<String,byte[]> serviceData = obj.getServiceData();
		        	if (!serviceData.isEmpty()) {
		        		
		        		// Get Bluetooth Device Mac Address
		        		String mac = obj.getAddress();
	        			
		        		// Get all service profiles returned by getServiceData() 
		        		Set<String> keys = serviceData.keySet();
		        		
		        		// for each profile try get the data and post it to EventAdmin
		        		for (String key : keys) {
		        			
		        			// Retrieves data for each service profile...
		        			byte[] rawData = serviceData.get(key);
		        			
			    			// ...create a new map object to post...
			    			Map<String,Object> data = new HashMap<String, Object>();  
			            	
			    			// ...add information retrieved from bluetooth device and...
			    			data.put(mac, rawData);
			    			
			    			// ... finally, post it to EventAdmin
			    			objectOutputStream.writeObject(data);	
		        		}

		        	}
		        }
		        manager.stopDiscovery(); 
		        Thread.sleep(1000);
			}
			catch (BluetoothException e)
			{
				System.out.println("Bluetooth exception. Stack trace: ");
				e.printStackTrace();
			}
			catch (Exception e)
			{
				System.out.println("Generic exception. Stack trace: ");
				e.printStackTrace();
			}
			
		} while (!done);
		
		System.out.println("Closing connection");
		consoleIn.close();
		socket.close();
	}
	
	private static List<BluetoothDevice> getBluetoothDevices() throws Exception {
		
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		List<BluetoothDevice> list = null;
		
        for (int i = 0; i < 15; ++i){
        	// Get bluetooth devices that are broadcasting
            list = manager.getDevices();
            
            Thread.sleep(1000);
        }
        
        //TODO: LOG	
        System.out.println("Devices found: ");
        for (BluetoothDevice device : list) {
          System.out.println(device.getAddress() + " | " + device.getName());
    	}		
      
		return list;
	}
}
