package com.ipleiria.mcs.datacollector.update.sensors;

import java.util.HashMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class Activator implements BundleActivator
{
	
	private static final String TOPIC_BASE        = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_NEW_SENSORS = TOPIC_BASE + "sensors";
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("[UPDATE] Updating sensors.json");
		sendData(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("[UPDATE] Stopping...");
		context.ungetService(context.getServiceReference(EventAdmin.class.getName()));
	}
	
	private void sendData(BundleContext context) {
		ServiceReference eventAdminRef = context.getServiceReference(EventAdmin.class.getName());
		if (eventAdminRef != null) {
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("payload", "changed");
			EventAdmin eventAdmin = (EventAdmin) context.getService(eventAdminRef);
			Event newEvent = new Event(TOPIC_NEW_SENSORS, props);
			System.out.println("[UPDATE] Ready to send data: " + newEvent);
			eventAdmin.sendEvent(newEvent);
		}
	}
}
