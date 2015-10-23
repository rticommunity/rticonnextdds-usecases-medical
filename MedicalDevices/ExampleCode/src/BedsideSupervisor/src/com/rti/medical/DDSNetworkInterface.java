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

import java.util.ArrayList;

import com.rti.dds.infrastructure.Duration_t;
import com.rti.medical.generated.Alarm;
import com.rti.medical.generated.AlarmTopic;
import com.rti.medical.generated.DevicePatientMapping;
import com.rti.medical.generated.DevicePatientMappingTopic;
import com.rti.medical.generated.ICE_QOS_LIBRARY;
import com.rti.medical.generated.QOS_PROFILE_ALARM;
import com.rti.medical.generated.QOS_PROFILE_PARTICIPANT;
import com.rti.medical.generated.QOS_PROFILE_PARTICIPANT_NO_MULTICAST;
import com.rti.medical.generated.QOS_PROFILE_PATIENT_DEVICES;
import com.rti.medical.generated.QOS_PROFILE_STREAMING;

import ice.Numeric;
import ice.NumericTopic;

public class DDSNetworkInterface {
	
	// --- Private fields --- //
	
	// DDSCommunicator class, responsible for creating a DomainParticipant, 
	// the DDS object that discovers other DDS objects.
	private final DDSCommunicator _communicator;
	
	// DataReader for Numeric data
	private final GenericDataReader<Numeric> _numericReader;
	

	// DataReader for Patient-Device mapping data
	private final PatientValueDataReader _patientDevicesReader;
	
	// DataWriter for sending Alarms
	private final GenericDataWriter<Alarm> _alarmDataWriter;

	// --- Public Methods --- //
	
	// ------------------------------------------------------------------------
	// Instantiates the DDS Communicator object. Uses the DDS Communicator to 
	// instantiate a DDS DomainParticipant, and the DataReaders for the
	// Numeric data type that is defined in the ice.idl file.  
	// 
	// ------------------------------------------------------------------------
	public DDSNetworkInterface(boolean multicastAvailable) throws Exception {
		
		_communicator = new DDSCommunicator();
		String profileName = null;

		// Specify a QoS profile for the DomainParticipant depending on whether
		// multicast is available on this network or not.
		if (multicastAvailable) {
			profileName = QOS_PROFILE_PARTICIPANT.VALUE;
		} else {
			profileName = QOS_PROFILE_PARTICIPANT_NO_MULTICAST.VALUE;
		}
		
		// Specify a list of XML QoS files to load
		ArrayList<String> qosFiles = new ArrayList<String>();
		qosFiles.add("file://../Config/qos_profiles.xml");

		// Create the DomainParticipant using those QoS files.
		_communicator.createParticipant(5,
				qosFiles, 
				ICE_QOS_LIBRARY.VALUE,
				profileName);
		
		// --- Create a Numeric Data Reader --- //
		// This uses a topic name that has been defined as a constant in the
		// ice.idl file.
		_numericReader = new GenericDataReader<Numeric>(
				_communicator, NumericTopic.VALUE,
				Numeric.class,
				ICE_QOS_LIBRARY.VALUE,
				QOS_PROFILE_STREAMING.VALUE);
				
		
		// --- Create a Patient-Device mapping Data Reader --- //
		// This uses a topic name that has been defined as a constant in the
		// patient.idl file.
		_patientDevicesReader = new PatientValueDataReader(
				_communicator, DevicePatientMappingTopic.VALUE,
				DevicePatientMapping.class,
				ICE_QOS_LIBRARY.VALUE,
				QOS_PROFILE_PATIENT_DEVICES.VALUE);
		
		_alarmDataWriter = new GenericDataWriter<Alarm> (
				_communicator, AlarmTopic.VALUE,
				Alarm.class, ICE_QOS_LIBRARY.VALUE,
				QOS_PROFILE_ALARM.VALUE);
	}
	
	// ------------------------------------------------------------------------
	// Add a listener to the Numeric DataReader that receives Numeric updates
	// ------------------------------------------------------------------------
	public void addNumericListener(SampleListener<Numeric> listener) {
		_numericReader.addListener(listener);
	}
	
	// ------------------------------------------------------------------------
	// Block the current thread until numerics become available, or a timeout
	// has occurred.
	// ------------------------------------------------------------------------
	public void waitForNumericData(Duration_t waitTime) 
			throws InterruptedException {
		_numericReader.waitForData(waitTime);
	}

	public PatientValueDataReader getPatientDevicesReader() {
		return _patientDevicesReader;
	}
	
	public GenericDataReader<Numeric> getNumericReader() {
		return _numericReader;
	}
	
	public GenericDataWriter<Alarm> getAlarmWriter() {
		return _alarmDataWriter;
	}

}
