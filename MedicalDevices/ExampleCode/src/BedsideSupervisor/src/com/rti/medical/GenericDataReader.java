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

package com.rti.medical;

import java.util.ArrayList;

import com.rti.dds.infrastructure.ConditionSeq;
import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.RETCODE_TIMEOUT;
import com.rti.dds.infrastructure.StatusCondition;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.infrastructure.WaitSet;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.SampleInfoSeq;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.dds.topic.Topic;
import com.rti.dds.util.Sequence;

// A class that receives data of type T.
//
// 
//
//
public class GenericDataReader<T> {

	// --- Private members --- //
	private final DDSCommunicator _communicator;
	protected final DataReader _reader;
	private final WaitSet _waitSet;
	private final Sequence _dataSeq;
	private final SampleInfoSeq _infoSeq;
	private final int _maxSamplesPerTake;
	private StatusCondition _statusCondition;
	private ArrayList<SampleListener<T>> _sampleListenerList;

	// --- Constructor --- //
	
	// This constructor takes in a DDS communicator that contains a DDS
	// DomainParticipant, along with a Topic name and the type class for the
	// type that this DataReader will read.  Note that the type the reader 
	// receives over the network and reads must be an RTI built-in type, or
	// must have been generated from IDL using the rtiddsgen code generator.
	// The DDSCommunicator class assumes this network data type has a related
	// TypeSupport class that helps register the data type with a
	// DomainParticipant so the DomainParticipant knows how to convert it to a
	// platform-independent network format.
	public GenericDataReader(DDSCommunicator communicator,
			String topicName,
			Class<T> typeClass,
			String qosLibrary,
			String qosProfile) throws Exception {

		_maxSamplesPerTake = 256;
		_communicator = communicator;

		// --- Creating the Topic for this DataReader --- //
		String typeClassName = typeClass.getName();
		Topic topic = _communicator.createTopic(topicName, typeClass);
		Subscriber subscriber = communicator.createSubscriber();
		 	 		 
		_reader = subscriber.create_datareader_with_profile(topic, 
				qosLibrary, qosProfile, null, 
				StatusKind.STATUS_MASK_NONE);

		_waitSet = new WaitSet();
		_statusCondition = _reader.get_statuscondition();
		_statusCondition.set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);
		_waitSet.attach_condition(_statusCondition);

		Class<?> sequenceClass = Class.forName(typeClassName + "Seq");
		_dataSeq = (Sequence) sequenceClass.newInstance();
		_infoSeq = new SampleInfoSeq();

		_sampleListenerList = new ArrayList<SampleListener<T>>();
	}
	
	public void addListener(SampleListener<T> listener) {
		_sampleListenerList.add(listener);
	}


	protected void finalize() {
		_waitSet.delete();
	}
	
	// --- Wait for new data --- //


	public void waitForData(Duration_t timeToWait) throws InterruptedException {

		// Try to date the data, if there is no data, wait and try again.
		if (!takeData()) {
		
			ConditionSeq activeConditions = new ConditionSeq();
			
			try {
				_waitSet.wait(activeConditions, timeToWait);
			} catch (RETCODE_TIMEOUT e) {
				return;
			}
			
			if (activeConditions.get(0) == _statusCondition) {
				takeData();
			}
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	private boolean takeData() {
		
		try {
			_reader.take_untyped(_dataSeq, _infoSeq, 
						_maxSamplesPerTake, 
						SampleStateKind.ANY_SAMPLE_STATE, 
						ViewStateKind.ANY_VIEW_STATE, 
						InstanceStateKind.ANY_INSTANCE_STATE);
			
			for (int i = 0; i < _dataSeq.size(); i++) {
				for (int j = 0; j < _sampleListenerList.size(); j++) {
					_sampleListenerList.get(j).processSample((T)_dataSeq.get(i));
				}
			}

		} catch(RETCODE_NO_DATA noData) {
			// Not a problem, continue to wait for new data to arrive.
		    return false;
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
			return false;
		} finally {
			_reader.return_loan_untyped(_dataSeq, _infoSeq);
		}
		
		return true;
		
	}
	
}
