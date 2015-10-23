package com.rti.medical;

import java.util.ArrayList;
import java.util.List;

import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.SampleInfoSeq;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.medical.generated.DevicePatientMapping;
import com.rti.medical.generated.DevicePatientMappingSeq;

public class PatientValueDataReader
	extends GenericDataReader<DevicePatientMapping> {

	public PatientValueDataReader(DDSCommunicator communicator, String topicName, 
			Class<DevicePatientMapping> typeClass, String qosLibrary,
			String qosProfile) throws Exception {
		super(communicator, topicName, typeClass, qosLibrary, qosProfile);
	}


	public int getPatient(String device_id) {
		
		DevicePatientMapping instanceHolder = new DevicePatientMapping();
		instanceHolder.device_id = device_id;
		InstanceHandle_t handle =
				_reader.lookup_instance_untyped(instanceHolder);
		
		if (handle.is_nil()) {	
			return -1;
		} 
		
		DevicePatientMappingSeq patientDevices = 
				new DevicePatientMappingSeq();
		patientDevices.setMaximum(256);
		SampleInfoSeq sampleInfos = 
				new SampleInfoSeq();
		sampleInfos.setMaximum(256);
		try {
			_reader.read_instance_untyped(patientDevices, sampleInfos, 
					ResourceLimitsQosPolicy.LENGTH_UNLIMITED, handle, 
					SampleStateKind.ANY_SAMPLE_STATE, 
					ViewStateKind.ANY_VIEW_STATE,
					InstanceStateKind.ANY_INSTANCE_STATE);
		} catch (Exception e) {
			// It is normal to have an exception when there is no data, yet.
		}
		
		if (patientDevices.size() > 0) {
            for(int i = 0; i < patientDevices.size(); ++i) {
            	SampleInfo info = (SampleInfo)sampleInfos.get(i);

            	if (info.valid_data) {	
            		DevicePatientMapping map = 
            				(DevicePatientMapping)patientDevices.get(i);
            		return map.patient_id;
            	}
            	
            }
                		
		}
		
	
		return -1;
	}
	
	@Override
	public void waitForData(Duration_t timeToWait) throws InterruptedException {
		System.out.println("Somebody's calling waitForData on the wrong reader");
	}
}
