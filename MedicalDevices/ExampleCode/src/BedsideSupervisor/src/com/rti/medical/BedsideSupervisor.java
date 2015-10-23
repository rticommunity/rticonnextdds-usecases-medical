package com.rti.medical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.RETCODE_TIMEOUT;
import com.rti.medical.generated.Alarm;

import ice.Numeric;

public class BedsideSupervisor {
	private static DDSNetworkInterface _dataInterface;
	private static NumericListener _numericListener;
	
    private static HashMap<Integer, ArrayList<Numeric>> _patientDeviceValues =
    		new HashMap<Integer,ArrayList<Numeric>>();
	
	public static void main(String[] args) {
		boolean multicastAvailable = true;
		
		try {
			// Is multicast available?
			if (args.length != 0) {
				if (args.length == 1 && args[0].equals("--no-multicast")) {
					multicastAvailable = false;
				} else {
					throw 
						new Exception("Invalid application argument.  Valid " +
									"arguments are: \n" +
									"\t--no-multicast:  Use QoS for a " + 
									"network with no multicast available.");
				}
			}

			// ----------------------------------------------------------------
			// This creates the DDS network interface to the HMI application. 
			// this receives alarm data over the network or shared memory using
			// RTI Connext DDS. It is responsible for discovering other
			// applications that send/receive data, and for reliably receiving 
			// alarm data.  This creates the Alarm DataReader that receives
			// alarms over the network.
			// ----------------------------------------------------------------
			_dataInterface = 
					new DDSNetworkInterface(multicastAvailable);

			_numericListener = new NumericListener();
			_dataInterface.addNumericListener(_numericListener);
			
			while (true) {
				try {
					MonitorPatient();
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			
		} catch (Exception e) {
		}
		
	}

	private static void MonitorPatient() {

		GetCurrentPatientValues();
		CheckForMultipleValuesOutOfRange();
		
		
	}
	
	private static void CheckForMultipleValuesOutOfRange() {

		// If multiple data values are out of range for a patient, 
		// send an alarm
		Integer valuesOutOfRange = 0;
		Iterator<Entry<Integer, ArrayList<Numeric>>> patientUpdates  = 
				_patientDeviceValues.entrySet().iterator();
		while (patientUpdates.hasNext()) {
		    Map.Entry<Integer,ArrayList<Numeric>> updatedEntry = 
		    		patientUpdates.next();
		    
		    Integer patientKey = updatedEntry.getKey();
		    ArrayList<Numeric> patientVitals = updatedEntry.getValue();
		    for( int i = 0; i < patientVitals.size(); i++) {
		    	
				if (patientVitals.get(i).value >= 100) {
					valuesOutOfRange++;
				}
			    // Taking a shortcut here for the tutorial because we know
			    // there are only two values being received for the patient
				if (valuesOutOfRange > 1) {
					System.out.println("Sending/updating alarm for patient ID: " +
						patientKey + " due to vitals: ");
					System.out.println(patientVitals.get(0).metric_id + ": " + patientVitals.get(0).value
						+ " " + patientVitals.get(1).metric_id + ": " + patientVitals.get(1).value);
					Alarm alarmData = new Alarm();
					alarmData.patient_id = patientKey;

					alarmData.device_alarm_values.setMaximum(2);
					alarmData.device_alarm_values.add(0, patientVitals.get(0));
					alarmData.device_alarm_values.add(1, patientVitals.get(1));

					try {
						_dataInterface.getAlarmWriter().write(alarmData);
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
		    }
		}

	}
	
	private static void GetCurrentPatientValues() {
		// Wait for Numeric data. Keep a per-patient map of the most-recent numeric data 
		// from the pulse oximeter and from the ECG.  
		// Look up the patient data related to those devices.
		// Check if they are out of bounds, if they are, write an alarm

		// Wait for updates to numeric data.
		Duration_t wait_time = new Duration_t();
		wait_time.sec = 3;
		wait_time.nanosec = 0;

		// Wait for the latest data updates for the 
		try {
			_dataInterface.getNumericReader().waitForData(wait_time);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		// Iterate over latest updates from devices.  Check which patients
		// these updates relate to, and if there are error states from
		// multiple devices, send an Alarm
		HashMap<String, Numeric> deviceValues = 
				_numericListener.getMostRecentDeviceValues();
		
		// Update all the patients' entries with the latest data
		Iterator<Entry<String,Numeric>> entries = 
				deviceValues.entrySet().iterator();
		while (entries.hasNext()) {
		    Map.Entry<String,Numeric> entry = entries.next();
		    String updatedDeviceKey = entry.getKey();
		    Numeric updatedDeviceValue = entry.getValue();

		    if (!updatedDeviceValue.metric_id.equals("MDC_PULS_OXIM_PULS_RATE") &&
		    		!updatedDeviceValue.metric_id.equals("MDC_PULS_RATE")) {
		    	continue;
		    }
		    Integer patientId = -1; 
		    patientId = _dataInterface.getPatientDevicesReader().getPatient(
		    		updatedDeviceKey);
		    
		    if (patientId == -1) {
		    	continue;
		    }
		  
			if (_patientDeviceValues.get(patientId) == null) {
				_patientDeviceValues.put(patientId, new ArrayList<Numeric>());
			}
			
			boolean valueUpdated = false;
			
			for (int i = 0; i < _patientDeviceValues.get(patientId).size(); 
					i++) {
				
				Numeric valueToUpdate = _patientDeviceValues.get(patientId).get(i);
				// If the newly-received value exists, update it
				if (valueToUpdate.unique_device_identifier.equals(
						updatedDeviceValue.unique_device_identifier)) {
					_patientDeviceValues.get(patientId).set(i, new Numeric(updatedDeviceValue));
					valueUpdated = true;
					break;
				}					
			}
			
			if (valueUpdated == false) {
				_patientDeviceValues.get(patientId).add(new Numeric(updatedDeviceValue));
			}
		}		
	}
	
}
