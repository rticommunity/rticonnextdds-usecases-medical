package com.rti.medical;

import java.lang.reflect.Method;

import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.publication.DataWriter;
import com.rti.dds.publication.Publisher;
import com.rti.dds.topic.Topic;
import com.rti.dds.topic.TypeSupport;

public class GenericDataWriter<T> {
    
    private final DataWriter _writer;
    
	// --- Private members --- //
	private final DDSCommunicator _communicator;
    
    private final Method _typeSupportCreateDataMethod;
    private final TypeSupport _typeSupport;    
    
    /**
     * Create a generic data writer to write data to DDS.
     * @param topicName The DDS topic name.
     * @param ddsTypeName The DDS type name (not the Java class name).
     * @param ddsInterfaceBean The DDS interface bean to use.
     * @throws Exception If any problem is encountered.
     */
    public GenericDataWriter(DDSCommunicator communicator,
			String topicName,
			Class<T> typeClass,
			String qosLibrary,
			String qosProfile)  throws Exception {
		_communicator = communicator;

		// --- Creating the Topic for this DataReader --- //
		String typeClassName = typeClass.getName();
		Topic topic = _communicator.createTopic(topicName, typeClass);
		Publisher publisher = communicator.createPublisher();
		 	 		 
		_writer = publisher.create_datawriter_with_profile(topic, 
				qosLibrary, qosProfile, null, 
				StatusKind.STATUS_MASK_NONE);
		
		Class<?> typeSupportClass;
		typeSupportClass = Class.forName(typeClassName + "TypeSupport");

        Method typeSupportGetInstance = 
            typeSupportClass.getDeclaredMethod("getInstance", 
                    (Class[]) null);
        _typeSupport = (TypeSupport) typeSupportGetInstance.invoke(null, 
                (Object[]) null);
		
		_typeSupportCreateDataMethod = 
				typeSupportClass.getDeclaredMethod(
                "create_data", (Class[]) null);
		
    }
    
    /**
     * Write a data instance to DDS.
     * @param instance The data to be written.
     * @throws Exception If a problem is encountered.
     */
    public void write(T instance) throws Exception {
    	_writer.write_untyped(instance, InstanceHandle_t.HANDLE_NIL);
    }
    
    /**
     * Create a new instance of the type associated with this generic data 
     * writer.
     * @return A data instance or null if a problem was encountered.
     */
    @SuppressWarnings("unchecked")
	public T createData() {
        T data = null;
        try {
            data = (T) _typeSupportCreateDataMethod.invoke(
            		_typeSupport,
                    new Object[]{});
        } catch(Exception e) {
        }
        return data;
    }
    
    public DataWriter getDataWriter() {
        return _writer;
    }
}
