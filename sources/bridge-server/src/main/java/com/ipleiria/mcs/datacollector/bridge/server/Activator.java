package com.ipleiria.mcs.datacollector.bridge.server;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;



public class Activator implements BundleActivator {

	EchoServer method;
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Bridge server started");
		method = new EchoServer();
		method.start();
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		method.stop();
		System.out.println("Bridge server stopped");
		
	}

}
