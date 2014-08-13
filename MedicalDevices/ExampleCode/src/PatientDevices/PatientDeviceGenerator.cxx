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
#include <vector>
#include <list>
#include <iostream>
#include "../Generated/patient.h"
#include "../Generated/patientSupport.h"
#include "../CommonInfrastructure/DDSTypeWrapper.h"
#include "DDSPatientDeviceInterface.h"

using namespace std;
using namespace com::rti::medical::generated;


void PrintHelp();

// ------------------------------------------------------------------------- //
// This application sends patient-device mapping information over RTI 
// Connext DDS. This is a simple application that uses pre-configured data
// for two patients.  
// 
// It sends the device-patient mapping for these two patients with the QoS 
// that is used for state data, meaning that this data is:
//    - Sent just once by the application
//    - Reliably delivered to current applications interested in this data
//    - Re-sent automatically by the middleware to any interested late-joining 
//      applications.
//
// ------------------------------------------------------------------------- //

int main(int argc, char *argv[])
{

	// Simple application with two patients hard-coded.
	int numPatients = 2;

	// Process the command-line arguments
	bool multicastAvailable = true;
	for (int i = 0; i < argc; i++)
	{
		if (0 == strcmp(argv[i], "--no-multicast"))
		{
			multicastAvailable = false;
		} else if (0 == strcmp(argv[i], "--help"))
		{
			PrintHelp();
			return 0;
		} else if (i > 0)
		{
			// If we have a parameter that is not the first one, and is not 
			// recognized, return an error.
			cout << "Bad parameter: " << argv[i] << endl;
			PrintHelp();
			return -1;
		}

	}

	// Create a mapping between patients and devices
	std::map<int, std::vector<std::string> > patientDeviceMappings;

	// Preconfigured data
	std::vector<std::string> patient1Devices;
	patient1Devices.push_back(
		std::string("o1sZLTxWBSlXPNPyvvK38UggdauMx4lbp9xE"));
	patient1Devices.push_back(
		std::string("gMz6b4SUv4qhOt70ZpF3qMuGTundJ6HPztOz"));
	patientDeviceMappings[0] = patient1Devices;
		std::vector<std::string> patient2Devices;
	patient2Devices.push_back(
		std::string("v6a29kIQPqjvp25N8TANEWYtfvH16d3u4sdD"));
	patient2Devices.push_back(
		std::string("B1SLmfV20qnJautN7ObxmSNVIOum8dipICcZ"));

	patientDeviceMappings[1] = patient2Devices;

	try 
	{

		// --------------------------------------------------------------------
		// This is the network interface for this application - this is what
		// actually sends the patient-device mapping information over the 
		// transport (shared memory or over the network).  Look into this class
		// to see what you need to do to implement an RTI Connext DDS 
		// application that writes data.
		DDSPatientDevicePubInterface patientDevicePub(multicastAvailable);


		DDS_Duration_t send_period = {0,100000000};

		cout << "Sending device-patient mappings over RTI Connext DDS" << endl;


		// Write all patient-device mappings up to the number specified
		for (int i = 0; i < numPatients; i++) 
		{

			// Allocate a device patient mapping structure
			DdsAutoType<DevicePatientMapping> patientDevice;

			for (int j = 0; j < patientDeviceMappings.size(); j++)
			{
				// We use integers as a placeholder for a real patient 
				// identifier
				patientDevice.patient_id = i + 1;
				strcpy(patientDevice.device_id, 
					patientDeviceMappings[i][j].c_str());

				// Write the data to the network.  This is a thin wrapper 
				// around the RTI Connext DDS DataWriter that writes data to
				// the network.
				patientDevicePub.Publish(patientDevice);
			}			


			NDDSUtility::sleep(send_period);
		}

		while (1) 
		{
			NDDSUtility::sleep(send_period);
		}
	}
	catch (string message)
	{
		cout << "Application exception: " << message << endl;
	}


	return 0;
}

void PrintHelp()
{
	cout << "Valid options are: " << endl;
	cout << 
		"    --no-multicast" <<
		"                 Do not use multicast " << 
		"(note you must edit XML" << endl <<
		"                                   " <<
		"config to include IP addresses)" 
		<< endl;

}
