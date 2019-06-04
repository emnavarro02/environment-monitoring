package com.ipleiria.mcs.datacollector.bridge.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

class ClientServiceThread extends Thread
{
	
	// private static final String TOPIC_BASE = "com/ipleiria/datacollector/discovery/";
	private static final String TOPIC_BROADCASTED_DATA = "BTRawData";
	
    Socket m_clientSocket;        
    int m_clientID = -1;
    boolean m_bRunThread = true;

    ClientServiceThread(Socket s, int clientID)
    {
        m_clientSocket = s;
        m_clientID = clientID;
    }

    public void run()
    {            
        // Obtain the input stream and the output stream for the socket
        BufferedReader in = null; 
        
        // Print out details of this connection
        System.out.println("Accepted Client : ID - " + m_clientID + " : Address - " + 
                         m_clientSocket.getInetAddress().getHostName());

        try
        {                                
            in = new BufferedReader(new InputStreamReader(m_clientSocket.getInputStream()));
            ObjectInputStream objectInputStream = new ObjectInputStream(m_clientSocket.getInputStream());
            
            // At this point, we can read for input and reply with appropriate output.
            // Run in a loop until m_bRunThread is set to false
            while(m_bRunThread)
            {            
            	@SuppressWarnings("unchecked")
            	Map<String,Object> message  = (Map<String, Object>) objectInputStream.readObject();
            	
            	System.out.println("Client says: " + message );
            	
            	//TODO: Create a stop condition based on the message sent by the client
                if(message == null)
                {
                    // Special command. Quit this thread
                	System.out.print("Stopping client thread for client : " + m_clientID);
                    m_bRunThread = false;   
                }
                else
                {
                	postMessage(message);
                }
            }
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
        finally
        {
            // Clean up
            try
            {                    
                in.close();
                // out.close();
                m_clientSocket.close();
                System.out.println("...Stopped");
            }
            catch(IOException e)
            {
                System.out.println("Attempting to stop bridge server.");
                System.out.println(e.getMessage());
            }
        }
    }

    public void postMessage(Map<String,Object> data) {
    	
    	Bundle bundle = FrameworkUtil.getBundle(getClass());
    	BundleContext context = bundle.getBundleContext();
    	ServiceReference serviceReference = context.getServiceReference(EventAdmin.class.getName());

    	if (serviceReference != null) {
			EventAdmin eventAdmin = (EventAdmin) context.getService(serviceReference);
			Event newEvent = new Event(TOPIC_BROADCASTED_DATA, data);
			System.out.println("Ready to send data: " + newEvent);
			eventAdmin.postEvent(newEvent);
		}
    }
}
