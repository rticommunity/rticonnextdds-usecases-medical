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
#include "DDSPatientDeviceInterface.h"
#include "../Generated/profiles.h"

using namespace com::rti::medical::generated;

// ----------------------------------------------------------------------------
// The DDSPatientDevicePubInterface is the network interface to the whole 
// application.  This creates a DataWriter in order to send patient-device
// mapping data over the network (or shared memory) to other applications that  
// are interested in knowing which devices are monitoring which patient.
//
// This interface is built from:
// 1. Network data types and topic names defined in the IDL file
// 2. XML configuration files that describe the QoS profiles that should be 
//    used by individual DataWriters and DataReaders.  These describe the 
//    movement and persistence characteristics of the data (how reliable should
//    this be?), as well as other QoS such as resource limits.
// 3. The code itself creates DataWriters, and selects which QoS profile to use
//    when creating the DataWriters.
// 
// Writing patint-device mapping data:
// -----------------------------------
// This application sends patient-device mapping data, configured to act as 
// state data (or last-value cache).  This will reliably deliver the patient-
// device data to both existing and late-joining applications that subscribe
// to patient-device data.
//
// For information on the data type, please see the patient.idl and ice.idl files.  
//
// For information on the quality of service for state data, please
// see the qos_profiles.xml file.
// ------------------------------------------------------------------------- //

DDSPatientDevicePubInterface::DDSPatientDevicePubInterface(
	bool multicastAvailable) 
{

	_communicator = new DDSCommunicator();

	std::vector<std::string> xmlFiles;

	// Adding the XML files that contain profiles used by this application
	xmlFiles.push_back(
		"file://../../../src/Config/qos_profiles.xml");

	std::string participantProfile;
	std::string participantLibrary;

	// Configuring this application for multicast or no multicast.  Note that
	// if you have no multicast, you will have to edit the XML QoS 
	// configuration to add the IP addresses of applications you want to 
	// discover and communicate with.
	if (multicastAvailable) 
	{
		participantProfile = QOS_PROFILE_PARTICIPANT;
	} else 
	{
		participantProfile = QOS_PROFILE_PARTICIPANT_NO_MULTICAST;
	}

	// Create a DomainParticipant
	// Start by creating a DomainParticipant.  Generally you will have only
	// one DomainParticipant per application.  A DomainParticipant is
	// responsible for starting the discovery process, allocating resources,
	// and being the factory class used to create Publishers, Subscribers, 
	// Topics, etc.  Note:  The string constants with the QoS library name and 
	// the QoS profile name are configured as constants in the .idl file.  The
	// profiles themselves are configured in the .xml file.
	if (NULL == _communicator->CreateParticipant(5, xmlFiles, 
				ICE_QOS_LIBRARY, QOS_PROFILE_PATIENT_DEVICES)) 
	{
		std::stringstream errss;
		errss << "Failed to create DomainParticipant object";
		throw errss.str();
	}

	// Create a Publisher
	// This application only writes data, so we only need to create a
	// publisher.  
	// Note that one Publisher can be used to create multiple DataWriters
	DDS::Publisher *pub = _communicator->CreatePublisher();

	if (pub == NULL) 
	{
		std::stringstream errss;
		errss << "Failed to create Publisher object";
		throw errss.str();
	}


	// Creating a Topic
	// The Topic object is the description of the data that you will be 
	// sending. It associates a particular data type with a name that 
	// describes the meaning of the data.  Along with the data types, and
	// whether your application is reading or writing particular data, this
	// is the data interface of your application.

	// This topic has the name DevicePatientMappingTopic - a constant  
	// string that is defined in the .idl file.  (It is not required that
	// you define your topic name in IDL, but it is a best practice for
	// ensuring the data interface of an application is all defined in one 
	// place. You can register all topics and types up-front, if you nee
	DDS::Topic *topic = _communicator->CreateTopic<DevicePatientMapping>( 
		DevicePatientMappingTopic);


	// Create a DataWriter.  
	// This creates a single DataWriter that writes patient-device 
	// mapping data, with QoS  that is used for State Data.	Note: The string 
	// constants with the QoS library name and the QoS profile name are 
	// configured as constants in the .idl file.  The profiles themselves 
	// are configured in the .xml file.
	DDS::DataWriter *writer = pub->create_datawriter_with_profile(topic, 
		ICE_QOS_LIBRARY, QOS_PROFILE_PATIENT_DEVICES,
		NULL, DDS_STATUS_MASK_NONE);

	// Downcast the generic datawriter to a device-patient mapping DataWriter 
	_writer = DevicePatientMappingDataWriter::narrow(writer);

	if (_writer == NULL) 
	{
		std::stringstream errss;
		errss << 
			"Failure to create DevicePatientMapping writer. Inconsistent Qos?";
		throw errss.str();
	}

}

// ----------------------------------------------------------------------------
// Destructor.
// Deletes the DataWriter, and the Communicator object
DDSPatientDevicePubInterface::~DDSPatientDevicePubInterface()
{
	DDS::Publisher *pub = _writer->get_publisher();
	pub->delete_datawriter(_writer);
	_writer = NULL;

	delete _communicator;
}


// ----------------------------------------------------------------------------
// Sends the device-patient mapping over a transport (such as shared memory or
// UDPv4) This writes the DevicePatientMapping data using RTI Connext DDS to 
// any DataReader that shares the same Topic
bool DDSPatientDevicePubInterface::Publish(DdsAutoType<DevicePatientMapping> data)
{
	DDS_ReturnCode_t retcode = DDS_RETCODE_OK;
	DDS_InstanceHandle_t handle = DDS_HANDLE_NIL;

	// This actually sends the device-patient mapping data over the network.  
	retcode = _writer->write(data, handle);

	if (retcode != DDS_RETCODE_OK) 
	{
		return false;
	}

	return true;

}

// ----------------------------------------------------------------------------
// Sends a deletion message for the patient-device mapping data over a 
// transport (such as shared memory or UDPv4) This uses the unregister_instance
// call to notify other applications that this device is no longer monitoring 
// this patient, and the mapping between them should be deleted
bool DDSPatientDevicePubInterface::Delete(DdsAutoType<DevicePatientMapping> data)
{
	DDS_ReturnCode_t retcode = DDS_RETCODE_OK;
	DDS_InstanceHandle_t handle = DDS_HANDLE_NIL;

	// Note that the deletion maps to an "unregister" in the RTI Connext
	// DDS world.  This allows the instance to be cleaned up entirely, 
	// so the space can be reused for another instance.  If you call
	// "dispose" it will not clean up the space for a new instance - 
	// instead it marks the current instance disposed and expects that you
	// might reuse the same instance again later.
	retcode = _writer->unregister_instance(data, handle);

	if (retcode != DDS_RETCODE_OK)
	{
		return false;
	}

	return true;
}
