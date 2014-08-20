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
#include <tuple>
#include "../Generated/ice.h"

// ------------------------------------------------------------------------- //
// Constants
// ------------------------------------------------------------------------- //
#define PULSE_OXIMETER_PULSE_RATE "MDC_PULS_OXIM_PULS_RATE"
#define PULSE_RATE "MDC_PULS_RATE"
// ------------------------------------------------------------------------- //
//
// BedsideSupervisor:
//
// A class that:
//	- Uses the DDSNetworkInterface to monitor devices on the network, looking
//		for out-of-range values
//	- Uses the DDSNetworkInterface to receive data over the network that
//		associates patients with individual devices.
//	- When it sees muiltiple out-of-range values from two devices for a single 
//		patient, uses the DDSNetworkInterface to send an alarm.
//
// Note that the functionality in this class is a simplified version of what
// a real bedside monitor would do.  It assumes there are only two devices 
// monitoring a patient.  It also does not look for trends in the values
// or do any complex logic to see if the device values really indicate an 
// alarm.  This is meant to illustrate the capability of DDS to send and 
// receive real-time device data over the network, and automatically discover
// new devices and HMI applications.
//
// ------------------------------------------------------------------------- //
class BedsideSupervisor 
{
public:

	// --- Constructor ---
	BedsideSupervisor(bool multicastAvailable);

	// ------------------------------------------------------------------------
	// The logic for monitoring patients
	void MonitorPatients();


private:
	// --- Private members --- 

	// Used to receive device data and patient-device mapping data.
	// Used to send Alarm data.
	DDSNetworkInterface *_ddsNetworkInterface;

	// A mapping between a patient ID and their most recent device values
	// for two devices.  
	std::map<com::rti::medical::generated::PatientId, 
			std::tuple<DdsAutoType<ice::Numeric>, DdsAutoType<ice::Numeric> > > 
					_patientLastReadings;
};