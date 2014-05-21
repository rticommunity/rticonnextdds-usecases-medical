/*********************************************************************************************
(c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.    	                             
RTI grants Licensee a license to use, modify, compile, and create derivative works 
of the Software.  Licensee has the right to distribute object form only for use with RTI 
products.  The Software is provided “as is”, with no warranty of any type, including 
any warranty for fitness for any purpose. RTI is under no obligation to maintain or 
support the Software.  RTI shall not be liable for any incidental or consequential 
damages arising out of the use or inability to use the software.
**********************************************************************************************/
package com.rti.medical;

import java.util.List;

import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.RETCODE_TIMEOUT;


public class ICEAlarmDisplayApp {

	public static void main(String[] args) {

		try {
			// TODO: make this a parameter
			boolean multicastAvailable = true;
			
			DDSNetworkInterface dataInterface = 
					new DDSNetworkInterface(multicastAvailable);
		
			final ICEDisplay display = new ICEDisplay();
			
			// Listener that updates the display when an alarm arrives
			dataInterface.addAlarmListener(
					new AlarmListener(display));

			//Schedule a job for the event-dispatching thread:
	        //creating and showing this application's GUI.
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	    			
	    			display.setVisible(true);
	            }
	        });	
			
	        
			// Wait for alarms, and let the AlarmListener class (defined below)
	        // handle updating the display with alarm data.
			while (true) {
				try {
					Duration_t waitTime = new Duration_t(5,0);
					dataInterface.waitForAlarms(waitTime);
				} catch (RETCODE_TIMEOUT e) {
					// Not an error
				}

			}
			
		} catch (Exception e) {
			// TODO: Best option for exceptions?
			System.out.println("Exception here");
		}
		
	}
}
