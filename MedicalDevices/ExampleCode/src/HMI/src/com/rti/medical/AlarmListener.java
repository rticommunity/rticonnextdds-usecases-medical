/*******************************************************************************
 (c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.
 RTI grants Licensee a license to use, modify, compile, and create derivative
 works of the Software.  Licensee has the right to distribute object form only
 for use with RTI products.  The Software is provided "as is", with no warranty
 of any type, including any warranty for fitness for any purpose. RTI is under
 no obligation to maintain or support the Software.  RTI shall not be liable for
 any incidental or consequential damages arising out of the use or inability to
 use the software.
 ******************************************************************************/

package com.rti.medical;

import ice.Numeric;

import java.util.Vector;

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
		Vector<String> alarmData = new Vector<String>();
		alarmData.add(Integer.toString(sample.patient_id));
		alarmData.add(sample.alarmKind.toString());
		
		String columnData = new String();
		for (int i = 0; i < sample.device_alarm_values.size(); i++) {
			Numeric deviceNumericData = 
					(Numeric)sample.device_alarm_values.get(i);
			columnData += "DeviceID: ";
			columnData += deviceNumericData.unique_device_identifier;
			columnData += " Value: ";
			columnData += deviceNumericData.value;
			columnData += " ";
		}
		alarmData.add(columnData);
		
		_display.addOrUpdateAlarmData(alarmData);
	}
	
}