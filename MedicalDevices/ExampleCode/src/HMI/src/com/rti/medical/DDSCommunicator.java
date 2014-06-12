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


/** 
 * 
 * This class is used by the application's DDS interface to create the core 
 * communication objects, such as the DomainParticipant, Publisher and/or 
 * Subscriber.
 * 
 * The DomainParticipant is a DDS object responsible for:
 * <ul>
 * <li>Setting up network communications
 * <li>Discovering other DomainParticipants within the same domain (created
 * using the same domain ID number).
 * <li>Discovering the DataWriters and DataReaders belonging to those 
 * DomainParticipants
 * </ul>
 * 
 * @author rose
 *
 */
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

	/**
	 * Creates a DomainParticipant with default QoS and domain ID 0.
	 * 
	 * A DomainParticipant starts the DDS discovery process.  It creates
	 * several threads, sends and receives discovery information over one or 
	 * more transports, and maintains an in-memory discovery database of 
	 * remote DomainParticipants, remote DataWriters, and remote DataReaders
	 * Quality of Service can be applied on the level of the DomainParticipant.  
	 * This QoS controls the characteristics of:
	 *  
	 * <ol>
	 * <li>Transport properties such as which type of network (UDPv4, UDPv6, 
	 * shared memory) or which network interfaces it is allowed to use    
	 * <li>Which applications this discovers.  By default, apps will discover
	 * other DDS applications over multicast, loopback, and shared memory.
	 * <li>Resource limits for the DomainParticipant
	 * </ol>
	 * For more information on participant QoS, see the .xml files in the 
	 * Config directory

	 * @return The newly-created DomainParticipant
	 * @throws Exception
	 */

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
	/**
	 * Creates a DomainParticipant with default QoS in the specified domain
	 * 
	 * A DomainParticipant starts the DDS discovery process.  It creates
	 * several threads, sends and receives discovery information over one or 
	 * more transports, and maintains an in-memory discovery database of 
	 * remote DomainParticipants, remote DataWriters, and remote DataReaders
	 * Quality of Service can be applied on the level of the DomainParticipant.  
	 * This QoS controls the characteristics of:
	 *  
	 * <ol>
	 * <li>Transport properties such as which type of network (UDPv4, UDPv6, 
	 * shared memory) or which network interfaces it is allowed to use    
	 * <li>Which applications this discovers.  By default, apps will discover
	 * other DDS applications over multicast, loopback, and shared memory.
	 * <li>Resource limits for the DomainParticipant
	 * </ol>
	 * For more information on participant QoS, see the .xml files in the 
	 * Config directory

	 * @return The newly-created DomainParticipant
	 * @throws Exception
	 */	
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
	/**
	 * Creates a DomainParticipant with specified QoS and domain ID.
	 * 
	 * A DomainParticipant starts the DDS discovery process.  It creates
	 * several threads, sends and receives discovery information over one or 
	 * more transports, and maintains an in-memory discovery database of 
	 * remote DomainParticipants, remote DataWriters, and remote DataReaders
	 * Quality of Service can be applied on the level of the DomainParticipant.  
	 * This QoS controls the characteristics of:
	 *  
	 * <ol>
	 * <li>Transport properties such as which type of network (UDPv4, UDPv6, 
	 * shared memory) or which network interfaces it is allowed to use    
	 * <li>Which applications this discovers.  By default, apps will discover
	 * other DDS applications over multicast, loopback, and shared memory.
	 * <li>Resource limits for the DomainParticipant
	 * </ol>
	 * For more information on participant QoS, see the .xml files in the 
	 * Config directory

	 * @return The newly-created DomainParticipant
	 * @throws Exception
	 */	
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
	/**
	 * Creates a DomainParticipant with default QoS and domain ID 0.
	 * 
	 * A DomainParticipant starts the DDS discovery process.  It creates
	 * several threads, sends and receives discovery information over one or 
	 * more transports, and maintains an in-memory discovery database of 
	 * remote DomainParticipants, remote DataWriters, and remote DataReaders
	 * Quality of Service can be applied on the level of the DomainParticipant.  
	 * This QoS controls the characteristics of:
	 *  
	 * <ol>
	 * <li>Transport properties such as which type of network (UDPv4, UDPv6, 
	 * shared memory) or which network interfaces it is allowed to use    
	 * <li>Which applications this discovers.  By default, apps will discover
	 * other DDS applications over multicast, loopback, and shared memory.
	 * <li>Resource limits for the DomainParticipant
	 * </ol>
	 * For more information on participant QoS, see the .xml files in the 
	 * Config directory
	 * 
	 * @return The newly-created DomainParticipant
	 * @throws Exception
	 */

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
	/** Gets the DomainParticipant object 
	 * 
	 * @return the DomainParticipant object, or null if it has not been created
	 */
	public DomainParticipant getParticipant() {
		return _participant;	
	}

	// --- Creating a Publisher --- //
	
	// --------------------------------------------------------------------- //
	/** Creates a Publisher object.  This is used to create type-specific 
	 *  DataWriter objects in the application
	 *
	 * @return The newly-created Publisher
	 * @throws Exception
	 */
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
	/** Creates a Publisher object with specified QoS.  This is used to create 
	 * type-specific DataWriter objects in the application
	 * 
	 * @param qosLibrary
	 * @param qosProfile
	 * @return The newly-created Publisher
	 * @throws Exception
	 */
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
	/** 
	 * Gets the publisher object
	 * 
	 * @return The Publisher, or null if it was not created.
	 * @throws Exception
	 */
	public Publisher getPublisher() throws Exception {
		if (_pub == null) {
			createPublisher();
		}
		return _pub;
	}

	// --- Creating a Subscriber --- //

	// --------------------------------------------------------------------- //
	/** Creates a Subscriber object.  This is used to create type-specific 
	 *  DataReader objects in the application
	 *  
	 * @return The newly-created Subscriber
	 * @throws Exception
	 */

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
	/** Creates a Subscriber object with specified QoS.  This is used to  
	 *  create type-specific DataReader objects in the application
	 *  
	 * @param qosLibrary
	 * @param qosProfile
	 * @return The newly-created Subscriber
	 * @throws Exception
	 */
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
	/**
	 * Gets the Subscriber
	 * 
	 * @return The Subscriber, or null if it was not created
	 * @throws Exception
	 */
	public Subscriber getSubscriber() throws Exception {
		if (_sub == null) {
			return createSubscriber();
		}
		
		return _sub;
	}
	
	/**
	 * Creates Topics. 
	 * 
	 * Topics are objects in DDS that describe the meaning of data being sent.
	 * The data type may be simple and reusable, but the Topic gives that data
	 * a label that explains the meaning and context of that data.  DataWriters
	 * and DataReaders communicate when they have the same Topic names.  
	 * 
	 * @param topicName
	 * @param type
	 * @return A Topic for type T
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
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
