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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler ;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothManager;

public class Activator implements BundleActivator, EventHandler
{
	private static final String KEY_BLUETOOTH = "bluetooth";
	private static final String KEY_DATA      = "data";
	private static final String KEY_ID        = "id";
	private static final String KEY_LOCATION  = "location";
	private static final String KEY_SENSORS   = "sensors";
	
	private static final String TOPIC_BASE                = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_TEMPERATURE_HUMDITY = TOPIC_BASE + "TempHum";
	private static final String TOPIC_NEW_SENSORS         = TOPIC_BASE + "sensors";
	
	private static final String SERVICE_PROFILE = "00001805-0000-1000-8000-00805f9b34fb";
	private static final String JSON_FILE = "sensors.json";
	
	static boolean running = true;
	static boolean stop = false;
	
	public List<Map<String,Object>> validSensors;
	public List<Map> availableSensors;
	
	@Override
	public void start(BundleContext context) throws Exception{
		
		System.out.println("Subscribing to the topic " + TOPIC_NEW_SENSORS);
		startListener(context);
		
		validSensors = loadFilteredSensors(JSON_FILE);
		System.out.println("There is/are " + validSensors.size() + 
				" sensors in the list.\nValid sensors: " + validSensors);
		
		//TODO: Colocar isso num loop e ver se consigo detectar alteracoes no handler
		// Discovery if the sensors is available
		availableSensors = getAvailableSensors(validSensors);
		System.out.println("\nAvailable Sensors: " + availableSensors);
		
		new Thread(new Runnable() {
		    public void run() {
		    	do {
					if (!availableSensors.isEmpty()) {
						// Get data from BluetoothDevice
						for (int i = 0; i < availableSensors.size(); i ++ ) {
							System.out.println("\nGetting data from sensor: " + availableSensors.get(i).get(KEY_ID));
							Map<String,Object> sensorData = getData((BluetoothDevice)availableSensors.get(i).get(KEY_BLUETOOTH));
							System.out.println(sensorData);
							
							//send raw data
							if (sensorData != null) {
								Map<String, Object> fullData = new HashMap<>();
								fullData.put(KEY_ID, availableSensors.get(i).get(KEY_ID));
								fullData.put(KEY_LOCATION, availableSensors.get(i).get(KEY_LOCATION));
								fullData.put(KEY_DATA, fullData);
								System.out.println();
								
								sendData(context, fullData);
							}
						}
					}
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						stop = true;
						e.printStackTrace();
						
					}
				} while (!stop);
		    }
		}).start();
	}

	private void startListener(BundleContext context) {
		Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put(EventConstants.EVENT_TOPIC, TOPIC_NEW_SENSORS);
		context.registerService(EventHandler.class.getName(), this, properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		try {
			manager.stopDiscovery();
		}
		catch (BluetoothException e){
			System.out.println("Bluetooth manager couldn't be stopped right now. The error is: \n" +e.getLocalizedMessage());
		}
		context.ungetService(context.getServiceReference(EventAdmin.class.getName()));
		
		System.out.println("Goodbye");
	}
	
	// My Functions
	
	/**
	 * This function try to load a JSON file with the sensors that should be considerate to the 
	 * environment data gathering.
	 * @param  fileName : full path to the JSON file 
	 * @return List<Map<String,Object>> : A list of Maps with all the sensors 
	 * */
	private List<Map<String,Object>> loadFilteredSensors(String fileName) {
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
		
		JSONObject jsonObject = new JSONObject(jsonAsString);
		JSONArray arr = jsonObject.getJSONArray(KEY_SENSORS);
		
		List<Map<String,Object>> validSensors = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++)
		{
			Map<String, Object> sensorsMap = new HashMap<>();
			String id = arr.getJSONObject(i).getString(KEY_ID);
			String location = arr.getJSONObject(i).getString(KEY_LOCATION);
			sensorsMap.put(KEY_ID, id);
			sensorsMap.put(KEY_LOCATION, location);
			
			validSensors.add(sensorsMap);
		}
		
		return validSensors;
	}

	
	
	private List<Map> getAvailableSensors(List<Map<String,Object>> validSensors) throws InterruptedException {
		
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		boolean discoveryStarted = manager.startDiscovery();
		System.out.println("Discovery started?: " + (discoveryStarted ? true : false));
		
		List<Map> filteredList = new ArrayList<Map>();
		
		for (int i = 0; i < validSensors.size(); i++) {
			Map<Object, Object> sensorsMap = new HashMap<>();
			
			String id = (String) validSensors.get(i).get(KEY_ID);	
			
			System.out.println("\nChecking if sensor " + id + " is available.");
			BluetoothDevice sensor = getDevice(id);
			
			if (sensor != null) {
				System.out.println("Sensor " + sensor.getAddress() + " with name '"+ sensor.getName() +"' has been found. Updating info.");
				
				System.out.println("\nInformation exposed by sensor:");
				System.out.println("UUIDs:            " + sensor.getUUIDs());
				for (String s : sensor.getUUIDs()) {
					System.out.println("   UUID: " + s );
				}

				//DEBUG DIALOG:
				System.out.println("Appearance:       " + sensor.getAppearance());
				System.out.println("BluetoothClass:   " + sensor.getBluetoothClass());
				System.out.println("RSSI:             " + sensor.getRSSI());
				System.out.println("TxPower:          " + sensor.getTxPower());
				System.out.println("BluetoothType:    " + sensor.getBluetoothType());
				System.out.println("Connected:        " + sensor.getConnected());
				System.out.println("Adapter:          " + sensor.getAdapter());
				System.out.println("ServicesResolved: " + sensor.getServicesResolved());
				System.out.println("Services:         " + sensor.getServices());
				System.out.println("ServiceData:      " + sensor.getServiceData());
				System.out.println("");
			
				sensorsMap.put(KEY_ID,validSensors.get(i).get(KEY_ID));
				sensorsMap.put(KEY_LOCATION,validSensors.get(i).get(KEY_LOCATION));
				sensorsMap.put(KEY_BLUETOOTH, sensor);
				
				filteredList.add(sensorsMap);
			}
			else {
				System.out.println("Sensor " + id + " not reacheble by Bluetooth adapter.");
			}
			System.out.println("\nAvailable sensors list updated.\nUpdated list: " + filteredList);
		}
		
		// Once the device objects are retrieved, we stop the "Search Mode"
		System.out.println("----------------------------------");
		System.out.println("Stopping the Bluetooth discover...");
		try {
			manager.stopDiscovery();
		}
		catch (BluetoothException e) {
			System.err.println("Could not stop the discovery right now."); 
		}
		System.out.println("");
		
		Lock lock = new ReentrantLock();
		Condition cv = lock.newCondition();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				running = false;
				lock.lock();
				try {
					cv.signalAll();
				}
				finally {
					lock.unlock();
				}
			}	
		});
		return filteredList;
	}
	
	private BluetoothDevice getDevice(Object object) throws InterruptedException {
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		BluetoothDevice sensor = null;
		
		for (int i = 0; i < 15; i++) {
			// Get bluetooth devices that are broadcasting
			List<BluetoothDevice> list = manager.getDevices();
			
			if (list == null) {
				return null;
			}
			
			// if the address of the discovered device matches with our required address, 
			// return the discovered object;
			for(BluetoothDevice device : list) {
				if (device.getAddress().equals(object)) {
					sensor = device;
				}
			}
			if (sensor != null) {
				return sensor;
			}
			Thread.sleep(4000);
		}
		return null;
	}
	
	private Map<String,Object> getData(BluetoothDevice sensor){
		
		Map<String,Object> data = new HashMap<String,Object>();
		Map<String, byte[]> serviceData = new HashMap<>();
		serviceData = sensor.getServiceData();
		byte[] payload = serviceData.get(SERVICE_PROFILE);
		
		if (payload != null) {
			float temperature = unpackData(payload[0],payload[1],payload[2]) / 100;
			float pressure    = unpackData(payload[3],payload[4], payload[5]) / 100;
			float height      = unpackData(payload[6],payload[7], payload[8]) / 100;
			int battery       = payload[9] & 0XFF;
			
			data.put("temperature", temperature);
			data.put("pressure", pressure);
			data.put("heigh", height);
			data.put("battery",battery);
			return data;
		}
		return null;
	}

	/**
	 * This method converts the Byte array retrieved from the sensors to a relevant information of
	 * temperature, pressure, height and battery.
	 * 
	 * @param  byte, byte, byte
	 * @return int
	 * */
	private int unpackData(byte b1, byte b2, byte b3) {
		 if ((b1 & 0x08) == 0x00)
		    {
		        return (b1 & 0x0F) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
		    }
		    else
		    {
		        int temp = ((byte)b1) << 16 | ((byte)b2) << 8 | (byte)b3;
		        ArrayList<Integer> bits = new ArrayList<>();
		        for (int i = 0; i < 20; i++)
		        {
		            bits.add(0x01 & (temp >> i));
		        }
		        for (int i = 0; i < 20; i++)
		        {
		            bits.set(i, bits.get(i) == 0 ? 1 : 0);
		        }
		        temp = 0;
		        for (int i = 0; i < 20; i++)
		        {
		            temp += (Math.pow(2, i) * bits.get(i));
		        }

		        // +1
		        temp += 1;
		        temp = 0 - temp;

		        return temp;
		    }
	}
	
	//TODO: acho que sera mais facil fazer esse bundle cuidar do processamento da mensagem e do envio. 
	// Alterar esse metodo para processar a mensagem
	private void sendData(BundleContext context, Map<String, ?> data) {
		ServiceReference eventAdminRef = context.getServiceReference(EventAdmin.class.getName());
		if (eventAdminRef != null) {
			EventAdmin eventAdmin = (EventAdmin) context.getService(eventAdminRef);
			Event newEvent = new Event(TOPIC_TEMPERATURE_HUMDITY, data);
			System.out.println("Ready to send data: " + newEvent);
			eventAdmin.sendEvent(newEvent);
		}
	}
		
	public void handleEvent(Event event) {
		System.out.println("Detected changes on the sensors list.");
		loadFilteredSensors(JSON_FILE);
		
		try {
			getAvailableSensors(validSensors);
		} catch (InterruptedException e) {
			System.out.println("Error while getting available sensors.\nError message: ");
			System.out.print(e.getLocalizedMessage());
		}
	}	


}

