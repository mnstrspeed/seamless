package nl.tomsanders.seamless.dsi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;

import nl.tomsanders.util.Observable;

public class OldInstanceManager
{	
	private static ConcurrentHashMap<Class<? extends Observable<?>>, Reference<? extends Observable<?>>> instances =
			new ConcurrentHashMap<Class<? extends Observable<?>>, Reference<? extends Observable<?>>>();
	
	@SuppressWarnings("unchecked")
	public synchronized static <T extends Observable<T>> Reference<T> getInstance(Class<T> type)
	{
		if (OldInstanceManager.instances.containsKey(type))
		{
			return (Reference<T>)OldInstanceManager.instances.get(type);
		}
		else
		{
			Reference<T> instance = OldInstanceManager.createInstance(type);
			OldInstanceManager.instances.put(type, instance);
			
			return instance;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Observable<T>> void updateInstance(Class<T> type, T instance)
	{
		if (OldInstanceManager.instances.containsKey(type))
		{
			// External update
			Reference<T> reference = (Reference<T>)OldInstanceManager.instances.get(type);
			reference.setModel(instance);
		}
		else
		{
			// Launch new instance (process) on local machine
			Reference<T> reference = new Reference<T>(instance);
			
			OldInstanceManager.instances.put(type, reference);
			new InstanceMonitor<T>(reference);
 		}
	}

	private static <T extends Observable<T>> Reference<T> createInstance(
			Class<T> type)
	{
		Constructor<T> constructor = OldInstanceManager.getDefaultConstructor(type);
		try
		{
			T instance = constructor.newInstance();
			Reference<T> reference = new Reference<T>(instance);
			new InstanceMonitor<T>(reference);
			
			return reference;
		}
		catch (Exception ex)
		{
			throw new RuntimeException("Unable to create instance", ex);
		}
	}
	
	private static <T> Constructor<T> getDefaultConstructor(Class<T> type) 
			throws SecurityException
	{
		try
		{
			return type.getDeclaredConstructor();
		}
		catch (NoSuchMethodException ex)
		{
			throw new RuntimeException("Default constructor not declared", ex);
		}
	}
	
	@SuppressWarnings("unused")
	private static <T> byte[] encodeInstance(T instance)
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try
		{
			ObjectOutput outputStream = new ObjectOutputStream(output);
			outputStream.writeObject(instance);
			outputStream.flush();
		} 
		catch (IOException ex)
		{
			throw new RuntimeException("Unable to encode instance", ex);
		}
		
		return output.toByteArray();
	}
	
}
