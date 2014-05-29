/*********************************************************************************************
(c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.    	                             
RTI grants Licensee a license to use, modify, compile, and create derivative works 
of the Software.  Licensee has the right to distribute object form only for use with RTI 
products.  The Software is provided “as is”, with no warranty of any type, including 
any warranty for fitness for any purpose. RTI is under no obligation to maintain or 
support the Software.  RTI shall not be liable for any incidental or consequential 
damages arising out of the use or inability to use the software.
**********************************************************************************************/
#include "DDSNetworkInterface.h"
#include "../CommonInfrastructure/DDSTypeWrapper.h"
#include "../CommonInfrastructure/OSAPI.h"
#include "../Generated/ice.h"
#include "../Generated/iceSupport.h"
#include "../Generated/alarm.h"
#include "../Generated/alarmSupport.h"
#include "../Generated/profiles.h"
#include "../Generated/patient.h"
#include "../Generated/patientSupport.h"


using namespace DDS;
using namespace ice;
using namespace com::rti::medical::generated;

// ----------------------------------------------------------------------------
// This creates the DDSNetworkInterface object.  

// The DDSNetworkInterface is comprised of:
// 1) One DDSCommunicator object - which is responsible for creating all 
//    objects that may be shared by multiple DataWriters and DataReaders - 
//    essentially all the objects that are (usually) created only once, 
//    regardless of how many data streams the application is sending or 
//    receiving.
//    The objects that are (usually) created once generally include:
//      1) DomainParticipant objects.  Typically an application has only one,
//         unless it must communicate in multiple domains.
//      2) Publisher and/or Subscriber.  Typically an application has at most 
//	       one of each.
//      3) Topics.  A topic with a particular name may be created only once per
//         DomainParticipant, and can be shared between multiple DataWriters
//         and DataReaders.
//      4) Types.  These must be registered and unregistered exactly once with
//         the DomainParticipant.
// 2) Any DataWriters and DataReaders that comprise the network interface
//    of the application.
DDSNetworkInterface::DDSNetworkInterface(bool multicastAvailable)
{ 					 
	std::vector<std::string> qosFileNames;

	// Adding the XML files that contain profiles used by this application
	qosFileNames.push_back(
		"file://../../../src/Config/qos_profiles.xml");

	// Class with code for creating the basic objects for DDS communication.
	_communicator = new DDSCommunicator();

	std::string participantProfile;
	std::string participantLibrary;

	if (multicastAvailable) 
	{
		participantProfile = QOS_PROFILE_PARTICIPANT;
	} else 
	{
		participantProfile = QOS_PROFILE_PARTICIPANT_NO_MULTICAST;
	}

	// Calling the DDSCommunicator class's CreateParticipant method.
	// This creates the DomainParticpant, the first step in creating a DDS
	// application.  This starts the discovery process.  For more information
	// on what the DomainParticipant is responsible for, and how to configure
	// it, see the DDSCommunicator class.
	if (NULL == _communicator->CreateParticipant(5, qosFileNames, 
									ICE_QOS_LIBRARY, 
									participantProfile)) 
	{
		std::stringstream errss;
		errss << "Failed to create DomainParticipant object";
		throw errss.str();
	}

	Subscriber *sub = GetCommunicator()->CreateSubscriber();

	// Create the DataReader that receives device numeric data.  The profiles
	// that are passed in define how the application will receive data,
	// and how much data will be kept by the middleware.  Look at the 
	// associated XML files for details. (Profile names are constant 
	// strings pre-defined in the .idl file)
	_numericReader = new NumericReader(this, sub, 
								ICE_QOS_LIBRARY,
								QOS_PROFILE_STREAMING);

	// Create the DataReader that receives device sample array data.  The 
	// profiles that are passed in define how the application will receive 
	// data, and how much data will be kept by the middleware.  Look at the 
	// associated XML files for details. (Profile names are constant 
	// strings pre-defined in the .idl file)
	_patientDevicesReader = new PatientDevicesReader(this, sub, 
								ICE_QOS_LIBRARY, 
								QOS_PROFILE_PATIENT_DEVICES);

	// Calling the DDSCommunicator class's CreatePublisher method.  
	// You do _not_ need to create one publisher per DataWriter.
	Publisher *publisher = _communicator->CreatePublisher();

	if (publisher == NULL) 
	{
		std::stringstream errss;
		errss << "Failed to create Publisher object";
		throw errss.str();
	}

	// Creating the application's AlarmWriter object.  
	// This could use the RTI Connext DDS writer directly as a way to write, 
	// but this example wraps the DDS DataWriter to simplify the API slightly.  
	_alarmWriter = new AlarmWriter(this, publisher,
									ICE_QOS_LIBRARY, 
									QOS_PROFILE_ALARM);
	if (_alarmWriter == NULL) 
	{
		std::stringstream errss;
		errss << "Failed to create AlarmWriter object";
		throw errss.str();
	}

}

// ----------------------------------------------------------------------------
// Destructor for the network interface. This deletes the readers, and the 
// communicator.  Notice that the DDS-specific cleanup code is in the 
// destructors of the individual reader and communicator objects.
DDSNetworkInterface::~DDSNetworkInterface()
{
	// Wake the readers up in case they are waiting for data
	_numericReader->NotifyWakeup();
	_patientDevicesReader->NotifyWakeup();

	delete _numericReader;
	delete _patientDevicesReader;
	delete _communicator;

}


// ----------------------------------------------------------------------------
// Creating the NumericReader object.
// This creates the DDS DataReader object that receives device numeric data  
// over one or more transports, and makes it available to the application.  
// When the DataReader object is first created, it starts the discovery process.
// The DataReader will start to receive data from DataWriters that are:
//  1) In the same domain
//  2) Have the same topic
//  3) Have compatible types
//  4) Have compatible QoS
// as soon as the discovery process has completed.
NumericReader::NumericReader(DDSNetworkInterface *ddsInterface, 
							Subscriber *sub,			
							const std::string &qosLibrary, 
							const std::string &qosProfile) 
{

	_mutex = new OSMutex();

	if (ddsInterface == NULL) 
	{
		std::stringstream errss;
		errss << "NumericReader(): bad parameter \"ddsInterface\"";
		throw errss.str();
	}

	_ddsInterface = ddsInterface;


	// Creating a Topic
	// The topic object is the description of the data that you will be 
	// sending. It associates a particular data type with a name that 
	// describes the meaning of the data.  Along with the data types, and
	// whether your application is reading or writing particular data, this
	// is the data interface of your application.

	// This topic has the name NumericTopic - a constant string that
	// is defined in the .idl file.  It is not required that you define your 
	// topic name in IDL, but it is a best practice for ensuring the data
	// interface of an application is all defined in one place.
	// Generally you register all topics and types up-front.
	Topic *topic = _ddsInterface->GetCommunicator()->CreateTopic<Numeric>( 
		NumericTopic);

	// Creating a DataReader
	// This DataReader will receive the numeric device data.
	// TODO: Describe how this will be used (i.e., kept in the queue, etc.)
	DataReader *reader = sub->create_datareader_with_profile(topic, 
		qosLibrary.c_str(),
		qosProfile.c_str(), 
		NULL, DDS_STATUS_MASK_NONE);
	if (reader == NULL)
	{
		std::stringstream errss;
		errss << "NumericReader(): failure to create DataReader.";
		throw errss.str();
	}

	 // Down casting to the type-specific reader
	_numericDeviceDataReader = NumericDataReader::narrow(reader);

	// This WaitSet object will be used to block a thread until one or more 
	// conditions become true.  In this case, there is a single condition that
	// will wake up the WaitSet when the reader receives data
	_waitSet = new WaitSet();
	if (_waitSet == NULL) 
	{
		std::stringstream errss;
		errss << "NumericReader(): failure to create WaitSet.";
		throw errss.str();
	}

	// Use this guard condition to wake up the thread waiting for data to 
	// notify it that the application is being shut down.
	_shutDownNotifyCondition = new GuardCondition;
	_waitSet->attach_condition(_shutDownNotifyCondition);

	// Use this status condition to wake up the thread when data becomes 
	// available
	_condition = _numericDeviceDataReader->get_statuscondition();
	_condition->set_enabled_statuses(DDS_DATA_AVAILABLE_STATUS);
	if (_condition == NULL) 
	{
		std::stringstream errss;
		errss << "NumericReader(): failure to initialize condition.";
		throw errss.str();
	}

	_waitSet->attach_condition(_condition);

}

// ----------------------------------------------------------------------------
// Destroying the NumericReader and the objects that are being used to 
// access it, such as the WaitSet and conditions.  Notice that we call 
// the DDS API delete_contained_entities() to ensure that all conditions
// associated with the DataReader are destroyed.  Topics are not destroyed by
// this call, because they may be shared across multiple DataReaders and
// DataWriters.
NumericReader::~NumericReader()
{
	_mutex->Lock();

	_waitSet->detach_condition(_condition);
	_waitSet->detach_condition(_shutDownNotifyCondition);
	delete _shutDownNotifyCondition;
	delete _waitSet;

	// _condition does not get deleted explicitly, because it belongs to the 
	// _numericDeviceDataReader.  Instead, it is deleted when you call 
	// delete_contained_entities here.
	_numericDeviceDataReader->delete_contained_entities();

	Subscriber *sub = _numericDeviceDataReader->get_subscriber();
	sub->delete_datareader(_numericDeviceDataReader);
	_numericDeviceDataReader = NULL;

	_mutex->Unlock();

	delete _mutex;

}


// This example is using an application thread to be notified when devices'
// numeric data arrives.  
// 
// There are three options for getting data from RTI Connext DDS:
// 1. Being notified in the application's thread of data arriving (as here).
//    This mechanism has slightly higher latency than option #2, but the
//    it is a safer option than using option #2, because you do not have to 
//    worry about the effect on the middleware's thread.   
//    This uses WaitSet objects to be notified of data arriving.
//    A simple of example of this can be found at: 
//    http://community.rti.com/examples/waitset-status-condition
// 2. Being notified in a listener callback of data arriving.
//    This has lower latency than using a WaitSet, but is more dangerous
//    because you have to worry about not blocking the middleware's thread.
// 3. Polling for data.
//    You can call read() or take() at any time to view or remove the data that
//    is currently in the queue. 
//    A simple example of this can be found at:
//    http://community.rti.com/examples/polling-read

void NumericReader::WaitForNumerics(
	std::vector< DdsAutoType<Numeric> > *numericUpdated,
	std::vector< DdsAutoType<Numeric> > *numericDeleted)
{

	ConditionSeq activeConditions;
	// How long to block for data at a time
	DDS_Duration_t timeout = {1,0};

	// Process flight plans if they exist, and do not wait for another
	// notification of new data
	if (true == ProcessNumericData(numericUpdated, numericDeleted))
	{
		return;
	}

	// Block thread for flight plan data to arrive
	DDS_ReturnCode_t retcode = _waitSet->wait(activeConditions, timeout);

	// Normal to time out 
	if (retcode == DDS_RETCODE_TIMEOUT) 
	{
		return;
	}
	if (retcode != DDS_RETCODE_OK) 
	{
		std::stringstream errss;
		errss << "WaitForNumerics(): error " << retcode << 
			" when receiving numeric data.";
		throw errss.str();
	}

	// If we have been woken up and notified that there was an event, we can
	// try to process numeric data.  Errors in processing numeric data will 
	// throw an exception
	ProcessNumericData(numericUpdated, numericDeleted);


}

// TODO: Update comment
// This method is taking data from the middleware's queue.
//
// In this example, we remove the data from the middleware's queue by calling
// take().  We do this to illustrate the common case where the data must be
// changed from one format (the network format) to another (the format that the
// radar library expects to receive its flight plan data).
// If the application is able to use the data directly without converting it to
// a different format, you can call read().  This leaves the data in the queue,
// and lets the application access it without having to copy it.

bool NumericReader::ProcessNumericData(
	std::vector< DdsAutoType<Numeric> > *updatedNumerics,
	std::vector< DdsAutoType<Numeric> > *deletedNumerics) 
{
	// Note: These two sequences are being created with a length = 0.
	// this means that the middleware is loaning memory to them, which
	// the application must return to the middleware.  This avoids 
	// having two separate copies of the data.
	NumericSeq numericSequence;
	SampleInfoSeq sampleInfoSequence;

	bool haveUpdates = false;

	DDS_ReturnCode_t retcode = DDS_RETCODE_OK;

	while (retcode != DDS_RETCODE_NO_DATA)
	{
		// This call removes the data from the middleware's queue
		retcode = _numericDeviceDataReader->take(numericSequence, 
			sampleInfoSequence);

		// If an error has occurred, throw an exception.  No data being
		// available is not an error condition
		if ((retcode != DDS_RETCODE_OK) && 
					(retcode != DDS_RETCODE_NO_DATA)) 
		{
			std::stringstream errss;
			errss << "ProcessNumericData(): error " << retcode << 
				" when retrieving device numeric data.";
			throw errss.str();
		}

		// TODO:  Update this comment based on QoS for streaming
		// Note, based on the QoS profile (history = keep last, depth = 1) and the 
		// fact that we modeled devices as separate instances, we can assume there
		// is only one entry per device.  So if a numeric update for a particular 
		// flight has been changed 10 times, we will  only be maintaining the most 
		// recent update to that flight plan in the middleware queue.
		for (int i = 0; i < numericSequence.length(); i++) 
		{
			// Return a value of true that numeric updates have been received
			haveUpdates = true;

			// Data may not be valid if this is a notification that an instance
			// has changed state.  In other words, this could be a notification 
			// that a writer called "dispose" to notify the other applications 
			// that the flight plan has moved to a dispose state.
			if (sampleInfoSequence[i].valid_data) 
			{
				// Making copies of this type into a vector controlled by the 
				// application.
				DdsAutoType<Numeric> numeric = numericSequence[i];
				updatedNumerics->push_back(numeric);
			} 
			else 
			{
				// This numeric device instance has been removed from the
				// system.  Alert the application to clean up resources related
				// to this device.
				DdsAutoType<Numeric> numericType = numericSequence[i];
				numericType.instance_id = 
						_numericDeviceDataReader->get_key_value(numericType, 
									sampleInfoSequence[i].instance_handle);
					deletedNumerics->push_back(numericType);
			}

		}

		// This returns the loaned memory to the middleware.
		_numericDeviceDataReader->return_loan(numericSequence, 
			sampleInfoSequence);

	}

	return haveUpdates;
}


// ----------------------------------------------------------------------------
// This wakes up the WaitSet for the Numeric DataReader, in case it is being
// used to wait until data is available.  This is used when shutting down to
// ensure that a thread that is querying data from the middleware will be woken
// up to shut down nicely.
void NumericReader::NotifyWakeup() 
{
	_shutDownNotifyCondition->set_trigger_value(true);
}

// ----------------------------------------------------------------------------
// Creating the PatientDevicesReader object.
// This creates the DDS DataReader object that receives devices' SampleArray  
// data over one or more transports, and makes it available to the application.
// When the DataReader object is first created, it starts the discovery process.  
// The DataReader will start to receive data from DataWriters that are:
//  1) In the same domain
//  2) Have the same topic
//  3) Have compatible types
//  4) Have compatible QoS
// as soon as the discovery process has completed.
PatientDevicesReader::PatientDevicesReader(DDSNetworkInterface *ddsInterface, 
						Subscriber *sub, 
						const std::string &qosLibrary, 
						const std::string &qosProfile) 
{

	_mutex = new OSMutex();
	ReturnCode_t retcode;

	if (ddsInterface == NULL) 
	{
		std::stringstream errss;
		errss << "PatientDevicesReader(): bad parameter \"ddsInterface\"";
		throw errss.str();
	}

	_ddsInterface = ddsInterface;

	const char *typeName = SampleArrayTypeSupport::get_type_name();
	retcode = SampleArrayTypeSupport::register_type(
			_ddsInterface->GetCommunicator()->GetParticipant(), typeName);
	if (retcode != RETCODE_OK) 
	{
		std::stringstream errss;
		errss << "PatientDevicesReader(): failure to register type. Registered twice?";
		throw errss.str();
	}

	// Creating a Topic
	// The topic object is the description of the data that you will be 
	// sending. It associates a particular data type with a name that 
	// describes the meaning of the data.  Along with the data types, and
	// whether your application is reading or writing particular data, this
	// is the data interface of your application.

	// This topic has the name SampleArrayTopic - a constant string that
	// is defined in the .idl file.  (It is not required that you define your 
	// topic name in IDL, but it is a best practice for ensuring the data
	// interface of an application is all defined in one place.
	// Generally you register all topics and types up-front.
	Topic *topic = _ddsInterface->GetCommunicator()
										->CreateTopic<DevicePatientMapping>(
												DevicePatientMappingTopic);

	// Creating a DataReader
	// This DataReader will receive the devices' sample array data, and will 
	// make it available for the application to query (read) or remove (take) 
	// from the reader's queue
	DataReader *reader = sub->create_datareader_with_profile(topic, 
		qosLibrary.c_str(),
		qosProfile.c_str(), 
		NULL, DDS_STATUS_MASK_NONE);
	if (reader == NULL)
	{
		std::stringstream errss;
		errss << "PatientDevicesReader(): failure to create DataReader.";
		throw errss.str();
	}

	// Down casting to the type-specific reader
	_reader = DevicePatientMappingDataReader::narrow(reader);

	// This WaitSet object will be used to block a thread until one or more 
	// conditions become true.  In this case, there is a single condition that
	// will wake up the WaitSet when the reader receives data, loses data, or
	// rejects data.  There is also a condition that wakes up the thread 
	// when it is time for the application to shut down.
	_waitSet = new WaitSet();
	if (_waitSet == NULL) 
	{
		std::stringstream errss;
		errss << "PatientDevicesReader(): failure to create WaitSet.";
		throw errss.str();
	}

	// Use this status condition to wake up the thread when data becomes 
	// available
	_condition = _reader->get_statuscondition();
	_condition->set_enabled_statuses(DDS_DATA_AVAILABLE_STATUS);
	if (_condition == NULL) 
	{
		std::stringstream errss;
		errss << "PatientDevicesReader(): failure to initialize condition.";
		throw errss.str();
	}

	_waitSet->attach_condition(_condition);	

	// Use this guard condition to wake up the thread waiting for data to 
	// notify it that the application is being shut down.
	_shutDownNotifyCondition = new GuardCondition;
	_waitSet->attach_condition(_shutDownNotifyCondition);

}

// ----------------------------------------------------------------------------
// Destroying the PatientDevicesReader and the objects that are being used to 
// access it, such as the WaitSet and conditions.  Notice that we call 
// the DDS API delete_contained_entities() to ensure that all conditions
// associated with the DataReader are destroyed.  Topics are not destroyed by
// this call, because they may be shared across multiple DataReaders and
// DataWriters.
PatientDevicesReader::~PatientDevicesReader()
{

	_mutex->Lock();

	_waitSet->detach_condition(_condition);
	_waitSet->detach_condition(	_shutDownNotifyCondition);
	delete _shutDownNotifyCondition;

	delete _waitSet;

	// _condition does not get deleted explicitly, because it belongs to the 
	// _reader.  Instead, it is deleted when you call delete_contained_entities
	// here.
	_reader->delete_contained_entities();
	Subscriber *sub = _reader->get_subscriber();
	sub->delete_datareader(_reader);

	_mutex->Unlock();

	delete _mutex;
}

// ----------------------------------------------------------------------------
// This call:
//   1) Queries the queue for patient/device mapping for a particular device
//      ID
//   2) Copies the value of a single patient ID into the the object that is 
//      passed in.  Due to the QoS settings, we know this has a history depth 
//      of one, so only the latest patient information for this device will 
//      be in the queue.
void PatientDevicesReader::GetPatient(ice::UniqueDeviceIdentifier deviceId, 
		PatientId *patientId)
{

	// Create a placeholder with only the key field filled in.  This will be
	// used to retrieve the device-patient mapping instance (if it exists).
	DdsAutoType<DevicePatientMapping> devicePatientMapping;
	devicePatientMapping.device_id = DDS_String_dup((char *)deviceId);

	// Look up the particular instance
	const DDS_InstanceHandle_t handle =
		_reader->lookup_instance(devicePatientMapping);

	// Not having a flight plan associated with this radar track is a normal 
	// case in this example application.  In a real-world application you may
	// want to throw an exception or return an error in the case where a flight
	// appears that has no associated flight plan.
	if (DDS_InstanceHandle_is_nil(&handle))
	{
		return;
	}

	DevicePatientMappingSeq devicePatientMappingSeq;
	SampleInfoSeq infoSeq;

	// Reading just the data for the flight plan we are interested in
	_reader->read_instance(devicePatientMappingSeq, infoSeq, 
			DDS_LENGTH_UNLIMITED,
			handle);


	if (devicePatientMappingSeq.length() > 0)
	{

		if (infoSeq[0].valid_data)
		{
			// Patient ID is really a long, so don't need to worry about a copy
			*patientId = devicePatientMappingSeq[0].patient_id;
		}
	}
	_reader->return_loan(devicePatientMappingSeq, infoSeq);

}


// ----------------------------------------------------------------------------
// This wakes up the WaitSet for the DevicePatientMapping DataReader, in case  
// it is being used to wait until data is available.  This is used when 
// shutting down to ensure that a thread that is querying data from the  
// middleware will be woken up to shut down nicely.
void PatientDevicesReader::NotifyWakeup() 
{
	_shutDownNotifyCondition->set_trigger_value(true);
}


// ----------------------------------------------------------------------------
// Creating the AlarmWriter object.
// This creates the DDS DataWriter object that sends alarm data  
// over one or more transports. 
// When the DataWriter object is first created, it starts the discovery process.
// The DataWriter will start to send data from DataReaders that are:
//  1) In the same domain
//  2) Have the same topic
//  3) Have compatible types
//  4) Have compatible QoS
// as soon as the discovery process has completed.

AlarmWriter::AlarmWriter(DDSNetworkInterface *ddsInterface, 
				Publisher *pub,
				const std::string &qosLibrary, 
				const std::string &qosProfile)
{

	if (ddsInterface == NULL) 
	{
		std::stringstream errss;
		errss << "AlarmWriter(): Bad parameter \"ddsInterface\"";
		throw errss.str();
	}
	_alarmWriter = NULL;

	_ddsInterface = ddsInterface;
	DomainParticipant *participant = 
		_ddsInterface->GetCommunicator()->GetParticipant();
		
	if (participant == NULL) 
	{
		std::stringstream errss;
		errss << "AlarmWriter(): participant has not been created";
		throw errss.str();
	}

	// The topic object is the description of the data that you will be 
	// sending. It associates a particular data type with a name that 
	// describes the meaning of the data.  Along with the data types, and
	// whether your application is reading or writing particular data, this
	// is the data interface of your application.

	// This topic has the name AlarmTopic - a constant string that is
	// defined in the alarm.idl file.  (It is not required that you define your 
	// topic name in IDL, but it is a best practice for ensuring the data
	// interface of an application is all defined in one place.
	// Generally you can register all topics and types up-front if
	// necessary.

	// This can be done at any time before creating the DataWriters and
	// DataReaders.  In some systems, this is done in a separate initialization
	// all at once - especially in applications that read and write the same 
	// topic
	Topic *topic = _ddsInterface->GetCommunicator()->CreateTopic<Alarm>(
		AlarmTopic);

	// --- Create a DataWriter --- 
	// Create the DDS DataWriter object that sends data over the network (or
	// shared memory)
	DataWriter *ddsWriter = 
		pub->create_datawriter_with_profile(topic, 		
								qosLibrary.c_str(),
								qosProfile.c_str(), 
								NULL, DDS_STATUS_MASK_NONE);


	// You cannot use a generic DataWriter to write data, you must cast it to
	// your type-specific DataWriter - in this case, a TrackDataWriter.
	_alarmWriter = AlarmDataWriter::narrow(ddsWriter);
	if (_alarmWriter == NULL) 
	{
		std::stringstream errss;
		errss << "AlarmWriter(): failure to create writer. Inconsistent Qos?";
		throw errss.str();
	}

}

// ----------------------------------------------------------------------------
// TODO: Comments
void AlarmWriter::PublishAlarm(DdsAutoType<Alarm> &alarm)
{
	InstanceHandle_t handle = DDS_HANDLE_NIL;
	bool handleSet = false;

	// TODO:  Explain concept of an instance
	// You can register the instance handle to get better 
	// throughput - however, this mostly makes sense if you are keeping
	// an object in your application where you can attach the instance
	// handle, or if you key fields are complex (more than 16 bytes long)

/*		 handle = _alarmWriter->register_instance(alarm);
*/

	// Write the alarm data onto the network (or over shared memory)
	DDS_ReturnCode_t retcode = _alarmWriter->write(alarm, handle);

	if (retcode != RETCODE_OK) 
	{
		std::stringstream errss;
		errss << "Write failure - resource limits hit?";
		throw errss.str();
	}


}

// ----------------------------------------------------------------------------
// TODO: Is this the right term?  Remove alarm?
void AlarmWriter::DeleteAlarm(DdsAutoType<Alarm> &alarm)
{
	InstanceHandle_t handle = DDS_HANDLE_NIL;
	
	// Retrieve the handle of the instance we were disposing
	handle = _alarmWriter->lookup_instance(alarm);

	// TODO:  Explain concept of an instance in this context

	// Note that DDS has two ways to indicate that an instance has gone away
	// it can unregister the instance or dispose it.  Also, by default when
	// the DataWriter unregisters an instance, it also disposes it.  If you
	// dispose and instance, the memory for the instance is not cleaned up,
	// with the expectation that it will be reused.  In this case, the 
	// instance IDs may change over time as new patients are admitted, so it
	// is better to unregister the instance than dispose it.
	DDS_ReturnCode_t retcode = 
		_alarmWriter->unregister_instance(alarm, handle);

	if (retcode != RETCODE_OK) 
	{
		std::stringstream errss;
		errss << "Write failure - resource limits hit?";
		throw errss.str();
	}

}