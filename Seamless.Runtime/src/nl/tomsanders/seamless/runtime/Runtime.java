package nl.tomsanders.seamless.runtime;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

import nl.tomsanders.seamless.config.NetworkConfiguration;
import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.networking.InstancePacket;
import nl.tomsanders.seamless.networking.InstancePacketConnection;
import nl.tomsanders.seamless.networking.InstancePacketType;
import nl.tomsanders.seamless.networking.InstanceRequestPacket;
import nl.tomsanders.seamless.networking.InstanceSyncPacket;
import nl.tomsanders.seamless.util.Observable;

/**
 * The Runtime is embedded in each application that uses
 * instance synchronization. It maintains a connection to
 * the InstanceServer to send and receive updates for
 * active instances.
 *
 */
public class Runtime 
{
	private static InstancePacketConnection instanceServerConnection;
	
	public synchronized static <T extends Observable<T> & Serializable> Reference<T> getInstance(Class<T> type)
	{
		return Runtime.getInstance(type, new DefaultInstanceFactory<T>(type));
	}
	
	@SuppressWarnings("unchecked")
	public synchronized static <T extends Observable<T> & Serializable> Reference<T> getInstance(Class<T> type, InstanceFactory<T> factory)
	{
		try
		{
			Log.v("Initializing connection with instance server");
			instanceServerConnection = new InstancePacketConnection(new Socket(
					NetworkConfiguration.LOCAL_HOST, 
					NetworkConfiguration.INSTANCESERVER_LOCAL_PORT));
			
			Log.v("Sending instance request to instance server");
			instanceServerConnection.send(new InstanceRequestPacket(type));
			
			Log.v("Waiting for response from instance server");
			InstancePacket response = instanceServerConnection.receive();
			
			T instance;
			// Either we receive an instance from the instance server
			// or we create our own
			if (response.getPacketType() == InstancePacketType.INSTANCE_SYNC)
			{
				Log.v("Instance server responded with instance");
				InstanceSyncPacket syncPacket = (InstanceSyncPacket)response;
				
				instance = (T)syncPacket.getInstance().getObject();
			}
			else if (response.getPacketType() == InstancePacketType.UNKNOWN_INSTANCE_RESPONSE)
			{
				Log.v("Instance server doesn't have the requested instance; "
						+ "creating new instance");
				
				instance = factory.createInstance();
				instanceServerConnection.send(new InstanceSyncPacket(instance));
				
				Log.v("Newly created instance transmitted to instance server");
			}
			else
			{
				instanceServerConnection.close();
				throw new RuntimeException("Invalid response from instance server");
			}
			
			// Create reference
			Reference<T> reference = new Reference<T>(instance);
			// Start monitoring the instance
			new InstanceObserver<T>(reference, type);
			
			// Keep listening for future updates from instance server
			instanceServerConnection.receiveAsync(new InstanceManagerPacketReceiver<T>(reference), true);
			
			return reference;
		} 
		catch (Exception ex) 
		{
			throw new RuntimeException("Failed to retrieve instance", ex);
		}
	}
	
	public synchronized static <T extends Observable<T> & Serializable> void updateInstance(Class<? extends T> type, T instance)
	{
		try
		{
			InstanceSyncPacket syncPacket = new InstanceSyncPacket(instance);
			if (instanceServerConnection != null)
			{
				instanceServerConnection.send(syncPacket);
			}
			else
			{
				throw new IllegalStateException("Connection with the instance server hasn't been initialized");
			}
		} 
		catch (IOException ex) 
		{
			throw new RuntimeException("Failed to update instance", ex);
		}
	}
	
	public synchronized static void exit()
	{
		if (instanceServerConnection != null)
		{
			try
			{
				instanceServerConnection.close();
			}
			catch (IOException e)
			{
			}
		}
	}
}
