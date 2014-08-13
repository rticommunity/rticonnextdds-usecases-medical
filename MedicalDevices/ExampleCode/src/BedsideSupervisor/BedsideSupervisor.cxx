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
#include <iostream>
#include "DDSNetworkInterface.h"
#include "BedsideSupervisor.h"
#include "../CommonInfrastructure/DDSTypeWrapper.h"
#include "../Generated/alarmSupport.h"
#include "../Generated/iceSupport.h"

void PrintHelp();

using namespace ice;
using namespace std;
using namespace com::rti::medical::generated;

BedsideSupervisor::BedsideSupervisor(bool multicastAvailable)
{
	// Create the DDS network interface that contains readers and writers of
	// the data that this bedside supervisor receives and sends.
	_ddsNetworkInterface = 
		new DDSNetworkInterface(multicastAvailable);

}

void BedsideSupervisor::MonitorPatients()
{
	// Wait for Numeric data. Keep a per-patient map of the most-recent numeric data 
	// from the pulse oximeter and from the ECG.  
	// Look up the patient data related to those devices.
	// Check if they are out of bounds, if they are, write an alarm

	// Wait for updates to numeric data.
	vector< DdsAutoType<Numeric> > updatedNumericValues;
	vector< DdsAutoType<Numeric> > deletedNumericDevices;
	_ddsNetworkInterface->GetNumericReader()->WaitForNumerics(
		&updatedNumericValues, &deletedNumericDevices);

	for (unsigned int i = 0; i < updatedNumericValues.size(); i++) 
	{
		//Numeric value = updatedNumericValues[i];
		PatientId id = -1; 
		_ddsNetworkInterface->GetPatientDevicesReader()->GetPatient(
				updatedNumericValues[i].unique_device_identifier, &id);

		if (_patientLastReadings.count(id) != 0)
		{
			std::tuple< DdsAutoType<Numeric>, DdsAutoType<Numeric> > readings = 
				_patientLastReadings[id];

			// Note: there may be metric IDs in the system that are not important
			// for what the bedside supervisor is monitoring.
			if (0 == strcmp(updatedNumericValues[i].metric_id, 
				PULSE_OXIMETER_PULSE_RATE)) 
			{
				std::get<0>(readings) = updatedNumericValues[i];
			}
			else if (0 == strcmp(updatedNumericValues[i].metric_id,
				PULSE_RATE))
			{
				std::get<1>(readings) = updatedNumericValues[i];
			}

			DdsAutoType<Numeric> first = std::get<0>(readings);
			DdsAutoType<Numeric> second = std::get<1>(readings);
			_patientLastReadings[id] = 
				std::tuple<Numeric, Numeric>(first, second);


			if (first.value > 100 &&
				second.value > 100) 
			{
				printf("Sending/updating alarm for patient ID: %d\n",
					id);
				DdsAutoType<Alarm> alarmData;
				alarmData.patient_id = id;

				alarmData.device_alarm_values.ensure_length(2,2);
				alarmData.device_alarm_values.set_at(0, std::get<0>(readings));
				alarmData.device_alarm_values.set_at(1, std::get<1>(readings));

				_ddsNetworkInterface->GetAlarmWriter()->PublishAlarm(alarmData);

			}
		}
		else
		{
			DdsAutoType<Numeric> first;
			DdsAutoType<Numeric> second;

            if (0 == strcmp(updatedNumericValues[i].metric_id,
                    PULSE_OXIMETER_PULSE_RATE))
            {
                    first = updatedNumericValues[i];
                    strcpy(second.metric_id, PULSE_RATE);
            } else if (0 == strcmp(updatedNumericValues[i].metric_id,
                    PULSE_RATE))
            {
                    second = updatedNumericValues[i];
                    strcpy(second.metric_id, PULSE_OXIMETER_PULSE_RATE);
            }

			_patientLastReadings[id] = 
				std::tuple<Numeric, Numeric>(first, second);

		}

	}

}


int main(int argc, char *argv[])
{

	cout << "Monitoring patients' devices over RTI Connext DDS, sending " 
		<< "alarms when multiple" << endl << "devices have out-of-range values" 
		<< endl;

	// Process arguments
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

	try 
	{
		// Create the new bedside supervisor class
		BedsideSupervisor *supervisor = 
			new BedsideSupervisor(multicastAvailable);

		while (true)
		{
			// Monitor patients
			supervisor->MonitorPatients();
		}

	} catch (string message)
	{
		cout << "Application exception: " << message << endl;
	}
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
