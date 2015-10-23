package com.rti.medical;

import java.util.HashMap;

import ice.Numeric;

public class NumericListener implements SampleListener<Numeric> {

	private HashMap<String, Numeric> _mostRecentDeviceValues = new 
			HashMap<String, Numeric>();
	


	@Override
	public void processSample(Numeric sample) {
		if (sample.metric_id.equals("MDC_PULS_OXIM_PULS_RATE")
				|| sample.metric_id.equals("MDC_PULS_RATE")) {
			_mostRecentDeviceValues.put(
					sample.unique_device_identifier,
					new Numeric(sample));
		}		
	}
	
	public HashMap<String, Numeric> getMostRecentDeviceValues() {
		return _mostRecentDeviceValues;
	}

}
