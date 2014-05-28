/*********************************************************************************************
(c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.    	                             
RTI grants Licensee a license to use, modify, compile, and create derivative works 
of the Software.  Licensee has the right to distribute object form only for use with RTI 
products.  The Software is provided �as is�, with no warranty of any type, including 
any warranty for fitness for any purpose. RTI is under no obligation to maintain or 
support the Software.  RTI shall not be liable for any incidental or consequential 
damages arising out of the use or inability to use the software.
**********************************************************************************************/
package com.rti.medical;

import com.rti.medical.generated.Alarm;

class AlarmListener implements SampleListener<Alarm> {

	/**
	 * 
	 */
	private final ICEDisplay _display;
	
	public AlarmListener(ICEDisplay display) {
		_display = display;
	}
	
	@Override
	public void processSample(Alarm sample) {
		_display.addAlarmString(new String(
				"Patient: " +
				sample.patient_id + " Alarm: " +
				sample.alarmKind.toString()));
		
	}
	
}