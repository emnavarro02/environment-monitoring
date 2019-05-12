package com.ipleiria.mcs.datacollector.discovery;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class Activator implements BundleActivator, EventHandler {
	private static final String KEY_DATA      = "data";
	private static final String KEY_ID        = "id";
	private static final String KEY_LOCATION  = "location";
	private static final String KEY_SENSORS   = "sensors";
	private static final String KEY_TEMPERATURE = "temperature";
	private static final String KEY_PRESSURE = "pressure";
	private static final String KEY_HEIGHT = "height";
	private static final String KEY_BATTERY = "battery";
	
	private static final String TOPIC_BASE                = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_TEMPERATURE_HUMDITY = TOPIC_BASE + "TempHum";
	private static final String TOPIC_BROADCASTED_DATA    = TOPIC_BASE + "RawData";
//	private static final String TOPIC_NEW_SENSORS         = TOPIC_BASE + "sensors";	
	
	private static final String JSON_FILE = "sensors.json";
	private List<Map<String,String>> sensors;
	private List<String> macList;
	
	ServiceReference eventAdminRef;
	EventAdmin eventAdmin;
	
	@Override
	public void start(BundleContext context) throws Exception {
		
		System.out.println("Discovery started");
		
		sensors = loadFilteredSensors(JSON_FILE);
		macList = getMacList();
		
		Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put(EventConstants.EVENT_TOPIC, TOPIC_BROADCASTED_DATA);
		System.out.println("Properties: " + properties);
		
		context.registerService(EventHandler.class.getName(), this, properties);
		
		eventAdminRef = context.getServiceReference(EventAdmin.class.getName());
		eventAdmin = (EventAdmin) context.getService(eventAdminRef);
	}
	
	/**
	 * Get a list of MAC Addresses from the Map object with sensors to be filtered
	 * 
	 * @return List<String>
	 * */
	private List<String> getMacList(){
		List<String> macs = new ArrayList<String>();
		for (Map<String,String> sensor : sensors) {
			macs.add(sensor.get(KEY_ID));
		}
		return macs;
	}
	
	/**
	 * Read a JSON file with required sensors info.
	 * 
	 * @param String
	 * @return List<Map<String,String>
	 * */
	private List<Map<String,String>> loadFilteredSensors(String fileName){
		
		StringBuilder sb = new StringBuilder();
		
		try {
			
			//Try to open the file 
			FileReader file = new FileReader(fileName);
			BufferedReader buffer = new BufferedReader(file);
			
			String line = buffer.readLine();
			
			while(line != null) {
				sb.append(line).append('\n');
				line = buffer.readLine();
			}
			buffer.close();			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		// load this file as a JSONObject.
		String jsonAsString = sb.toString();
		
		JSONObject jsonObject = new JSONObject(jsonAsString);
		JSONArray arr = jsonObject.getJSONArray(KEY_SENSORS);
		
		// Put it objects in a List of Maps: 
		List<Map<String, String>> validSensors = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++)
		{
			Map<String, String> sensorsMap = new HashMap<>();
			String id = arr.getJSONObject(i).getString(KEY_ID);
			String location = arr.getJSONObject(i).getString(KEY_LOCATION);
			sensorsMap.put(KEY_ID, id);
			sensorsMap.put(KEY_LOCATION, location);
			
			validSensors.add(sensorsMap);
		}
		
		return validSensors;
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		context.ungetService(eventAdminRef);
		System.out.println("Discovery stopped");
	}

	@Override
	public void handleEvent(Event event) {
		
		// System.out.println("New event on topic: " + event.getTopic());
		
		// Listens for events that arrived in a specific topic
		switch (event.getTopic()) {
		
		case TOPIC_BROADCASTED_DATA:
			System.out.println("Message Key: " + event.getPropertyNames()[0]);
			
			// Get MAC Address info in the message
			String id = event.getPropertyNames()[0];
			
			// Search by this ID in the list of MAC Address, so we don't need to process messages
			// from not required sensors.
			if(macList.contains(id)) {
				
				// Get the service data from the message
				System.out.println("Message key is on the MAC Address List. Retrieving data");
				byte[] message_data = (byte[]) event.getProperty(id);
				
				// Process the message
				System.out.println("Processing message for device with ID '" + id + "' and data " + message_data + "\n");
				processMessage(id, message_data);
			}
			break;		
		}	
	}
	
	/**
	 * This method process the message arrived on the EventAdmin Handler, converting the data in human readable
	 * information and associating with sensor data such as location and id. 
     * 
	 * @param  String, byte[]
	 * @return void
	 * */
	private void processMessage(String id, byte[] message_data) {
		System.out.println("Looking for the device in file.");
		
		// Convert the byte array data in a human readable information.
		Map<String, Object> data = new HashMap<>();
		float temperature = unpackData(message_data[0], message_data[1], message_data[2]) / 100;
		float pressure    = unpackData(message_data[3], message_data[4], message_data[5]) / 100;
		float height      = unpackData(message_data[6], message_data[7], message_data[8]) / 100;
		int battery       = message_data[9] & 0XFF;
		data.put(KEY_TEMPERATURE,temperature);
		data.put(KEY_PRESSURE,pressure);
		data.put(KEY_HEIGHT, height);
		data.put(KEY_BATTERY,battery);
		
		// Get the location of the sensor (which is configured in the JSON file).
		String location = "";
		for (Map<String,String> element : sensors) {
			if(element.containsValue(id)) {
				// System.out.println("element contains value.");
				// System.out.println(element.get(KEY_LOCATION).toString());
				location = element.get(KEY_LOCATION);
			}
		}
		
		// Create the full message data
		Map<String, Object> message = new HashMap<>();
		message.put(KEY_ID, id);
		message.put(KEY_LOCATION, location);
		message.put(KEY_DATA, data);
		
		// Send message to database
		System.out.println("Ready to send message: " + message);
		Event event = new Event(TOPIC_TEMPERATURE_HUMDITY, message);
		eventAdmin.postEvent(event);
				
	}
	
	/**
	 * This method converts the Byte array retrieved from the sensors to a relevant information of
	 * temperature, pressure, height and battery.
	 * 
	 * @param  byte, byte, byte
	 * @return int
	 * */
	private int unpackData(byte b1, byte b2, byte b3) {
		if ((b1 & 0x08) == 0x00){
			return (b1 & 0x0F) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
		}
	    else{
	        
	    	int temp = ((byte)b1) << 16 | ((byte)b2) << 8 | (byte)b3;
	        ArrayList<Integer> bits = new ArrayList<>();
	        
	        for (int i = 0; i < 20; i++){
	            bits.add(0x01 & (temp >> i));
	        }
	        
	        for (int i = 0; i < 20; i++){
	            bits.set(i, bits.get(i) == 0 ? 1 : 0);
	        }
	        
	        temp = 0;
	        for (int i = 0; i < 20; i++){
	            temp += (Math.pow(2, i) * bits.get(i));
	        }

	        // +1
	        temp += 1;
	        temp = 0 - temp;

	        return temp;
	    }
	}
}
