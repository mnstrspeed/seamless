package nl.tomsanders.seamless.dsi;

import java.lang.reflect.Constructor;

public class DefaultInstanceFactory<T> implements InstanceFactory<T>
{
	private final Class<T> type;
	
	public DefaultInstanceFactory(Class<T> type)
	{
		this.type = type;
	}
	
	@Override
	public T createInstance() 
	{
		Constructor<T> constructor = this.getDefaultConstructor(this.type);
		try
		{
			return constructor.newInstance();
		}
		catch (Exception ex)
		{
			throw new RuntimeException("Unable to create instance", ex);
		}
	}
	
	private Constructor<T> getDefaultConstructor(Class<T> type) 
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
}