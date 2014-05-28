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

import java.util.ArrayList;

import com.rti.dds.infrastructure.Duration_t;
import com.rti.medical.generated.Alarm;
import com.rti.medical.generated.AlarmTopic;
import com.rti.medical.generated.ICE_QOS_LIBRARY;
import com.rti.medical.generated.QOS_PROFILE_ALARM;
import com.rti.medical.generated.QOS_PROFILE_PARTICIPANT;
import com.rti.medical.generated.QOS_PROFILE_PARTICIPANT_NO_MULTICAST;

public class DDSNetworkInterface {
	
	private final DDSCommunicator _communicator;
	private final GenericDataReader<Alarm> _reader;
	

	public DDSNetworkInterface(boolean multicastAvailable) throws Exception {
		
		_communicator = new DDSCommunicator();
		String profileName = null;
		
		if (multicastAvailable) {
			profileName = QOS_PROFILE_PARTICIPANT.VALUE;
		} else {
			profileName = QOS_PROFILE_PARTICIPANT_NO_MULTICAST.VALUE;
		}
		
		ArrayList<String> qosFiles = new ArrayList<String>();
		qosFiles.add("file://../Config/qos_profiles.xml");
		
		_communicator.createParticipant(5,
				qosFiles, 
				ICE_QOS_LIBRARY.VALUE,
				profileName);
		
		
		_reader = new GenericDataReader<Alarm>(
				_communicator, AlarmTopic.VALUE,
				Alarm.class,
				ICE_QOS_LIBRARY.VALUE,
				QOS_PROFILE_ALARM.VALUE);
	}
	
	public void addAlarmListener(SampleListener<Alarm> listener) {
		_reader.addListener(listener);
	}
	
	public void waitForAlarms(Duration_t waitTime) 
			throws InterruptedException {
		_reader.waitForData(waitTime);
	}
}
