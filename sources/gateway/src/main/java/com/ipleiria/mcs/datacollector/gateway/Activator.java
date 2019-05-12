package com.ipleiria.mcs.datacollector.gateway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothManager;

/**
 * This bundle is the Mock implementation of the OSGi Gateway.
 * 
 * It listens to the BluetoothDevices and post all broadcasted data retrieved from 
 * bluetooth devices to an EventAdmin topic.
 *
 */
public class Activator implements BundleActivator
{

	private static final String TOPIC_BASE = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_BROADCASTED_DATA = TOPIC_BASE + "RawData";
	
	boolean running;
	Thread thread;
	
	public void start(BundleContext context) throws Exception, BluetoothException {
		
		System.out.println("Gateway started");
		
		running = true;
		
		thread = new Thread(new Runnable() {
			public void run() {
				do {
					try {
						BluetoothManager manager = BluetoothManager.getBluetoothManager();
						// boolean discoveryStarted = manager.startDiscovery();
				        // System.out.println("Discovery started: " + (discoveryStarted ? "true" : "false"));
				        
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
					            	sendData(context, data);	
				        		}

				        	}
				        }
				        manager.stopDiscovery(); 
				        Thread.sleep(1000);
					
					} catch (BluetoothException e) {
						System.out.print(e.getMessage());
					} catch (Exception e) {
						System.out.print(e.getMessage());
					}
				} while (running);
			}
		});
		
		thread.start();
	}
	

	@Override
	public void stop(BundleContext context) throws Exception {
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		try {
			
			manager.stopDiscovery();
			running = false;
			thread.stop();
		}
		catch (BluetoothException e) {
			System.out.println("Bluetooth manager couldn't be stopped right now. The error is: \n" +e.getLocalizedMessage());
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		System.out.println("Gateway stopped");
	}
	
	private List<BluetoothDevice> getBluetoothDevices() throws Exception {
		
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		List<BluetoothDevice> list = null;
		
        for (int i = 0; i < 15; ++i){
        	// Get bluetooth devices that are broadcasting
            list = manager.getDevices();
            
            Thread.sleep(4000);
        }
        
        //TODO: LOG	
        System.out.println("Devices found: ");
        for (BluetoothDevice device : list) {
          System.out.println(device.getAddress() + " | " + device.getName());
    	}		
      
		return list;
	}
	
	//TODO: Check the topic that gateway is posting
	private void sendData(BundleContext context, Map<String, ?> data) {
		ServiceReference eventAdminRef = context.getServiceReference(EventAdmin.class.getName());
		if (eventAdminRef != null) {
			EventAdmin eventAdmin = (EventAdmin) context.getService(eventAdminRef);
			Event newEvent = new Event(TOPIC_BROADCASTED_DATA, data);
			System.out.println("Ready to send data: " + newEvent);
			eventAdmin.postEvent(newEvent);
		}
	}
}
