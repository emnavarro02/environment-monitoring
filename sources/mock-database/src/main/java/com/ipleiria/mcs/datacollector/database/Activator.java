package com.ipleiria.mcs.datacollector.database;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This bundle is the Database interface on the project. 
 * 
 * It listens to posts in one EventAdmin topic and Handle these events
 * (in this case, it just prints the events)  
 * 
 * */
public class Activator implements BundleActivator, EventHandler {

	private static final String TOPIC_BASE = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_TEMPERATURE_HUMDITY = TOPIC_BASE + "TempHum";
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("starting database bundle...");
		startListender(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		stopListener(context);
		System.out.println("stopping database bundle...");
	}

	private void startListender(BundleContext context) {
		Dictionary<String,String> props = new Hashtable<String, String>();
		props.put(EventConstants.EVENT_TOPIC, TOPIC_TEMPERATURE_HUMDITY);
		context.registerService(EventHandler.class.getName(), this, props);
	}
	
	private void stopListener(BundleContext context) {
		context.ungetService(context.getServiceReference(EventAdmin.class.getName()));
	}

	@Override
	public void handleEvent(Event event) {
		System.out.println("\n[Database] New event: ");
		System.out.println("   Topic: " + event.getTopic());
		for (String s : event.getPropertyNames()) {
			System.out.println("   PropertyName: " + s);
			System.out.println("      Property: " + event.getProperty(s));
		}		
	}


}
