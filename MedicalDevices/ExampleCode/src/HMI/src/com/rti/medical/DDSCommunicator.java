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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.domain.DomainParticipantFactoryQos;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.topic.Topic;
import com.rti.dds.topic.TypeSupport;


// TODO: Javadoc
public class DDSCommunicator {

	
	// --- Private members ---
	
	// Used to create other DDS entities
	private DomainParticipant _participant;
	
	// Used to create DataWriters
	private Publisher _pub;

	// Used to create DataReaders
	private Subscriber _sub;
	
	
	// --- Constructor --- //
	public DDSCommunicator() {
		_participant = null;
		_pub = null;
		_sub = null;
	}

	// --- Finalization and Network Cleanup --- //
	protected void finalize() {
		if (_participant != null) {
			_participant.delete_contained_entities();
		}
		
		DomainParticipantFactory.get_instance().
				delete_participant(_participant);
	}

	// --- Public methods --- //
	
	// --- Creating a DomainParticipant --- //

	// A DomainParticipant starts the DDS discovery process.  It creates
	// several threads, sends and receives discovery information over one or 
	// more transports, and maintains an in-memory discovery database of 
	// remote DomainParticipants, remote DataWriters, and remote DataReaders

	// Quality of Service can be applied on the level of the DomainParticipant.  
	// This QoS controls the characteristics of:
	// 1. Transport properties such as which type of network (UDPv4, UDPv6, 
	//    shared memory) or which network interfaces it is allowed to use
	// 2. Which applications this discovers.  By default, apps will discover
	//    other DDS applications over multicast, loopback, and shared memory.
	// 3. Resource limits for the DomainParticipant
	//
	// For more information on participant QoS, see the .xml files in the 
	// Config directory


	// --------------------------------------------------------------------- //
	// Creating a DomainParticipant with a domain ID of zero
	public DomainParticipant createParticipant() throws Exception {
		_participant = 
			DomainParticipantFactory.get_instance().create_participant(
							0, 
							DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, 
							null, StatusKind.STATUS_MASK_NONE);

		if (_participant == null) {
			throw new Exception("Error creating DomainParticipant");			
		} 

		return _participant;
	}
	
	// --------------------------------------------------------------------- //
	// Creates a DomainParticipant with default QoS in the specified domain
	public DomainParticipant createParticipant(int domain) throws Exception {
		_participant = 
				DomainParticipantFactory.get_instance().
				create_participant(
						domain, 
						DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, 
						null, StatusKind.STATUS_MASK_NONE);

		if (_participant == null) {
			throw new Exception("Error creating DomainParticipant");
		} 

		return _participant;
	}
	
	
	// ------------------------------------------------------------------------- //
	// Creating a DomainParticipant with a specified domain ID and specified QoS 
	public DomainParticipant createParticipant(int domain, 
			String participantQosLibrary, 
			String participantQosProfile) throws Exception {
		
		_participant = 
			DomainParticipantFactory.get_instance()
				.create_participant_with_profile(
										domain, 
										participantQosLibrary, 
										participantQosProfile, 
										null, 
										StatusKind.STATUS_MASK_NONE);

		if (_participant == null) {
			throw new Exception("Error creating DomainParticipant");
		} 

		return _participant;

	}


	// --------------------------------------------------------------------- //
	// Creating a DomainParticipant with a specified domain ID, specified QoS 
	// file names, and specified QoS 
	public DomainParticipant createParticipant(int domain, 
		List<String>fileNames, 
		String participantQosLibrary, 
		String participantQosProfile) throws Exception {

		// Adding a list of explicit file names to the DomainParticipantFactory
		// This gives the middleware a set of places to search for the files
		DomainParticipantFactoryQos factoryQos = 
				new DomainParticipantFactoryQos();
		DomainParticipantFactory.get_instance().get_qos(factoryQos);
		factoryQos.profile.url_profile.setMaximum(fileNames.size());

		for (int i = 0; i < fileNames.size(); i++) {
			
			factoryQos.profile.url_profile.add(fileNames.get(i));
		}

		DomainParticipantFactory.get_instance().set_qos(factoryQos);
			
		// Actually creating the DomainParticipant
		_participant = 
			DomainParticipantFactory.get_instance()
				.create_participant_with_profile(
									domain, 
									participantQosLibrary, 
									participantQosProfile, 
									null, 
									StatusKind.STATUS_MASK_NONE);

		if (_participant == null) {
			throw new Exception("Failed to create DomainParticipant object");
		} 

		return _participant;

	}


	// --------------------------------------------------------------------- //
	// Get the DomainParticipant object 
	public DomainParticipant getParticipant() {
		return _participant;	
	}

	// --- Creating a Publisher --- //
	
	// --------------------------------------------------------------------- //
	// Creating a Publisher object.  This is used to create type-specific 
	// DataWriter objects in the application
	public Publisher createPublisher()	throws Exception {
		if (getParticipant() == null) {
			throw new Exception(
				"DomainParticipant null, communicator not properly initialized");
		}

		// Creating a Publisher.  
		// This object is used to create type-specific DataWriter objects that 
		// can actually send data.  
		// 
		_pub = getParticipant().create_publisher(
										DomainParticipant.PUBLISHER_QOS_DEFAULT, 
										null, StatusKind.STATUS_MASK_NONE);	

		if (_pub == null) {
			throw new Exception("Failed to create Publisher");
		}

		return _pub;
	}

	// ------------------------------------------------------------------------- //
	// Creating a Publisher object with specified QoS.  This is used to create 
	// type-specific DataWriter objects in the application
	public Publisher createPublisher(String qosLibrary, String qosProfile) 
			throws Exception {
		if (getParticipant() == null) {
			throw new Exception(
				"DomainParticipant NULL - communicator not properly " +
					"initialized");
		}

		// Creating a Publisher.  
		// This object is used to create type-specific DataWriter objects that 
		// can actually send data.  
		// 
		_pub = getParticipant().create_publisher_with_profile(
							qosLibrary, 
							qosProfile,
							null, StatusKind.STATUS_MASK_NONE);	

		if (_pub == null) {
			throw new Exception("Failed to create Publisher");
		}

		return _pub;
	}
	
	// --- Getting the Publisher --- //
	public Publisher getPublisher() throws Exception {
		if (_pub == null) {
			createPublisher();
		}
		return _pub;
	}

	// --- Creating a Subscriber --- //

	// --------------------------------------------------------------------- //
	// Creating a Subscriber object.  This is used to create type-specific 
	// DataReader objects in the application
	public Subscriber createSubscriber() throws Exception {
		if (getParticipant() == null) {
			throw new Exception(
				"DomainParticipant NULL - communicator not properly " +
					"initialized");
		}

		// Creating a Subscriber.  
		// This object is used to create type-specific DataReader objects that 
		// can actually receive data.  The Subscriber object is being created
		//  in the DDSCommunicator class because one Subscriber can be used to
		//  create multiple DDS DataReaders. 
		// 
		_sub = getParticipant().create_subscriber(
									DomainParticipant.SUBSCRIBER_QOS_DEFAULT, 
									null, StatusKind.STATUS_MASK_NONE);	

		if (_sub == null) {
			throw new Exception("Failed to create Subscriber");
		}

		return _sub;
	}

	// --------------------------------------------------------------------- //
	// Creating a Subscriber object with specified QoS.  This is used to  
	// create type-specific DataReader objects in the application
	public Subscriber createSubscriber(
			String qosLibrary, String qosProfile) throws Exception {

		if (getParticipant() == null) {
				throw new Exception("DomainParticipant NULL - " + 
								"communicator not properly initialized");
		}

		// Creating a Subscriber.  
		// This object is used to create type-specific DataReader objects that 
		// can actually receive data.  The Subscriber object is being created
		//  in the DDSCommunicator class because one Subscriber can be used to
		//  create multiple DDS DataReaders. 
		// 
		_sub = getParticipant().create_subscriber_with_profile(
							qosLibrary, 
							qosProfile, 
							null, StatusKind.STATUS_MASK_NONE);	
		if (_sub == null) {
			throw new Exception( "Failed to create Subscriber");
		}

		return _sub;

	}
	
	// --- Getting the Subscriber --- //
	public Subscriber getSubscriber() throws Exception {
		if (_sub == null) {
			return createSubscriber();
		}
		
		return _sub;
	}
	
    @SuppressWarnings("unchecked")
	public <T> Topic createTopic(String topicName, Class<T> type) 
			throws ClassNotFoundException, 
            NoSuchMethodException, SecurityException, IllegalAccessException, 
            IllegalArgumentException, InvocationTargetException, 
            InstantiationException {
        Class<T> typeClass = type;
        String typeClassName = typeClass.getName();
        
        Class<? extends TypeSupport> typeSupportClass = 
        		(Class<? extends TypeSupport>) Class.forName(
                typeClassName + "TypeSupport");
        Method getInstanceMethod = typeSupportClass.getMethod("get_instance");
        TypeSupport typeSupport = (TypeSupport) getInstanceMethod.invoke(null);
        
        Method getTypeName = typeSupportClass.getMethod("get_type_name");
        String typeName = (String) getTypeName.invoke(null);
        
        // register the type
        _participant.register_type(typeName, typeSupport, null);
        
        return _participant.create_topic(topicName, typeName, 
                DomainParticipant.TOPIC_QOS_DEFAULT, null, 
                StatusKind.STATUS_MASK_NONE);
		
	}
	
}
