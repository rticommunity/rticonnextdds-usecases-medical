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
#ifndef DDS_PATIENT_DEVICE_INTERFACE_H
#define DDS_PATIENT_DEVICE_INTERFACE_H

#include <sstream>
#include "../CommonInfrastructure/DDSCommunicator.h"
#include "../CommonInfrastructure/DDSTypeWrapper.h"
#include "../Generated/patient.h"
#include "../Generated/patientSupport.h"


// ----------------------------------------------------------------------------
//
// The patient device interface provides patient-device mapping data over the 
// network (or shared memory) to other applications that are interested knowing
// which device is monitoring which patient.
//
// Writing patient device data:
// ----------------------------
// This application data that indicates which medical device is monitoring 
// which patient.  This data is configured to behave as state data (or last-
// value cache).  This will reliably deliver each patient-device combination to
// both existing and late-joining applications that subscribe to patient-device
// data.
//
// For information on the patient-device data type, please see the 
// patient.idl and ice.idl files.  
//
// For information on the quality of service for state data, please
// see the qos_profiles.xml file.
//
// ----------------------------------------------------------------------------
class DDSPatientDevicePubInterface
{

public:

	// --- Constructor --- 
	// Initializes the interface, including creating a 
	// DomainParticipant, creating all publishers and subscribers, topics 
	// writers and readers.  Takes as input a vector of xml QoS files that
	// should be loaded to find QoS profiles and libraries.
	DDSPatientDevicePubInterface(bool multicastAvailable);

	// --- Destructor --- 
	~DDSPatientDevicePubInterface();

	// --- Getter for Communicator --- 
	// Accessor for the communicator (the class that sets up the basic
	// DDS infrastructure like the DomainParticipant).
	// This allows access to the DDS DomainParticipant/Publisher/Subscriber
	// classes
	DDSCommunicator *GetCommunicator() 
	{ 
		return _communicator; 
	}

	// --- Sends the device-patient mapping data ---
	// Uses DDS interface to send a patient-device efficiently over the network
	// or shared memory to interested applications subscribing to 
	// patient-device information.
	bool Publish(
			DdsAutoType<com::rti::medical::generated::DevicePatientMapping> 
				data);

	// --- Deletes the patient-device mapping---
	// "Deletes" the patient-device mapping from the system - removing the DDS  
	// instance from all applications.
	bool Delete(DdsAutoType<com::rti::medical::generated::DevicePatientMapping>
				data);

private:
	// --- Private members ---

	// Used to create basic DDS entities that all applications need
	DDSCommunicator *_communicator;

	// Device-patient mapping publisher specific to this application
	com::rti::medical::generated::DevicePatientMappingDataWriter *_writer;
};

#endif
