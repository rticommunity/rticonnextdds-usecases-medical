/*********************************************************************************************
(c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.    	                             
RTI grants Licensee a license to use, modify, compile, and create derivative works 
of the Software.  Licensee has the right to distribute object form only for use with RTI 
products.  The Software is provided “as is”, with no warranty of any type, including 
any warranty for fitness for any purpose. RTI is under no obligation to maintain or 
support the Software.  RTI shall not be liable for any incidental or consequential 
damages arising out of the use or inability to use the software.
**********************************************************************************************/
#ifndef DDS_NETWORK_INTERFACE_H
#define DDS_NETWORK_INTERFACE_H

#include <vector>
#include "../CommonInfrastructure/DDSCommunicator.h"
#include "../CommonInfrastructure/DDSTypeWrapper.h"

#include "../Generated/ice.h"
#include "../Generated/alarm.h"


class OSMutex;
class NumericReader;
class PatientDevicesReader;
class AlarmWriter;


// ------------------------------------------------------------------------- //
//
// DDSNetworkInterface:
// A class that sets up the DDS interface (the network interface) of this 
// GUI application, including creating appropriate DDS DataWriters, DDS
// DataReaders, and all other DDS objects.
//
// In this example, the GUI is only subscribing to data, so this class creates
// two DDS DataReaders and no DDS DataWriters.
//
// ------------------------------------------------------------------------- //
class DDSNetworkInterface  
{
public:

	// --- Constructor --- 
	// The constructor creates all the necessary DDS objects (in this case, 
	// a DomainParticipant, a Subscriber, objects wrapping two DDS 
	// DataReaders, a Publisher, and object wrapping two DDS DataWriters.)  
	// This also configures the XML QoS configuration files that should be 
	// used by the application.
	DDSNetworkInterface(bool multicastAvailable);

	// --- Destructor --- 
	~DDSNetworkInterface();

	// --- Getter for Communicator --- 
	// Accessor for the communicator (the class that sets up the basic
	// DDS infrastructure like the DomainParticipant).
	// This allows access to the DDS DomainParticipant/Publisher/Subscriber
	// classes
	DDSCommunicator *GetCommunicator() 
	{ 
		return _communicator; 
	}

	// --- Getter for the numeric medical data reader --- 
	// Accessor for the object that receives numeric device data from the 
	// network
	NumericReader *GetNumericReader() 
	{ 
		return _numericReader; 
	}

	// --- Getter for the patient device reader --- 
	// Accessor for the object that receives patient device updates from the 
	// network
	PatientDevicesReader *GetPatientDevicesReader() 
	{ 
		return _patientDevicesReader; 
	}

	// --- Getter for the sample array reader --- 
	// Accessor for the object that awnsa alarms over the network
	AlarmWriter *GetAlarmWriter() 
	{ 
		return _alarmWriter; 
	}


private:
	// --- Private members ---

	// Used to create basic DDS entities that all applications need
	DDSCommunicator *_communicator;

	// Numeric device data receiver specific to this application
	NumericReader *_numericReader;

	// Sample array device receiver specific to this application
	PatientDevicesReader *_patientDevicesReader;

	// Used to write Alarms to the network and delete alarms if they are no  
	// longer valid.
	AlarmWriter *_alarmWriter;
};


// ------------------------------------------------------------------------- //
//
// NumericReader:
// A wrapper for a DDS DataReader, that receives numeric device data
//
// ------------------------------------------------------------------------- //
class NumericReader 
{

public:

	// --- Constructor --- 
	// Subscribes to device numeric information
	NumericReader(DDSNetworkInterface *ddsInterface, DDS::Subscriber *sub, 
		const std::string &qosLibrary, const std::string &qosProfile);

	// --- Destructor --- 
	~NumericReader();

	// --- Wait for device numeric updates --- 
	// This example is waiting to be notified that the numeric data arriving 
	// from devices has been updated, or that a device has been removed from 
	// the system, and its numeric data should no longer be stored.
	void WaitForNumerics(
		std::vector< DdsAutoType<ice::Numeric> > *numericUpdated,
		std::vector< DdsAutoType<ice::Numeric> > *numericDeleted);

	// --- Remove numeric data from middleware queue
	bool ProcessNumericData(
		std::vector< DdsAutoType<ice::Numeric> > *numericUpdated,
		std::vector< DdsAutoType<ice::Numeric> > *numericDeleted); 


	// --- Wake up the reader thread if it is waiting on data ---
	void NotifyWakeup(); 

private:
	// --- Private members ---

	// Contains all the components needed to create the DataReader
	DDSNetworkInterface *_ddsInterface;

	// The DDS DataReader of numeric device data 
	ice::NumericDataReader *_numericDeviceDataReader;

	// The mechanisms that cause a thread to wait until device data
	// becomes available, and to be woken up when the data arrives
	DDS::WaitSet *_waitSet;
	DDS::StatusCondition *_condition;
	DDS::GuardCondition *_shutDownNotifyCondition;

	// Mutex for threading
	OSMutex *_mutex;

};

// ------------------------------------------------------------------------- //
//
// PatientDevicesReader:
// A wrapper for a DDS DataReader, that blocks an application thread, waiting
// to receive notifications about devices that are monitoring individual
// patients.
//
// ------------------------------------------------------------------------- //
class PatientDevicesReader 
{

public:

	// --- Constructor --- 
	// Subscribes to patient-device mapping information
	PatientDevicesReader(DDSNetworkInterface *ddsInterface, 
		DDS::Subscriber *sub, 
		const std::string &qosLibrary, 
		const std::string &qosProfile);

	// --- Destructor --- 
	~PatientDevicesReader();

	// --- Retrieve patient for device --- 
	// This example is not being notified in an application thread when a 
	// device is associated with a patient, and instead queries the middleware
	// to know which patient is being monitored by a device when it is 
	// interested in it.  It queries the middleware queue by patient ID.
	void GetPatient(ice::UniqueDeviceIdentifier deviceId, 
		com::rti::medical::PatientId *patientId);

	// --- Wake up the reader thre if it is waiting on data ---
	void NotifyWakeup();

private:
	// --- Private members ---

	// Contains all the components needed to create the DataReader
	DDSNetworkInterface *_ddsInterface;

	// The DDS DataReader of which devices monitor which patients 
	com::rti::medical::DevicePatientMappingDataReader *_reader;

	// The mechanisms that cause a thread to wait until patient device 
	// monitoring data becomes available, and to be woken up when the data 
	//arrives
	DDS::WaitSet *_waitSet;
	DDS::StatusCondition *_condition;
	DDS::GuardCondition *_shutDownNotifyCondition;

	// Mutex for threading
	OSMutex *_mutex;
};

class AlarmWriter {

public:

	// --- Constructor --- 
	AlarmWriter(DDSNetworkInterface *ddsInterface, 
				 DDS::Publisher *pub,
				const std::string &qosLibrary, 
				const std::string &qosProfile);

	// --- Destructor --- 
	~AlarmWriter();

	// TODO: change namespace to be more similar to TrackData (aka, generated)
	void PublishAlarm(DdsAutoType<com::rti::medical::Alarm> &alarm);

	// TODO: Is this the right term?  Remove alarm?
	void DeleteAlarm(DdsAutoType<com::rti::medical::Alarm> &alarm);

private:
	// Contains all the components needed to create the DataWriter
	DDSNetworkInterface *_ddsInterface;

	// DataWriter that sends Alarm updates 
	com::rti::medical::AlarmDataWriter *_alarmWriter;

};

#endif
