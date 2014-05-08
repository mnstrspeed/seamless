package nl.tomsanders.seamless.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.networking.InstancePacket;
import nl.tomsanders.seamless.networking.InstancePacketConnection;
import nl.tomsanders.seamless.networking.InstancePacketType;
import nl.tomsanders.seamless.networking.InstanceSyncPacket;
import nl.tomsanders.seamless.networking.ObjectConnection;
import nl.tomsanders.seamless.networking.ObjectConnection.ObjectReceiver;
import nl.tomsanders.seamless.util.Observable;
import nl.tomsanders.seamless.util.Observer;

public class Reference<T extends Observable<T> & Serializable> 
		extends Observable<Reference<T>> implements Observer<T>, Closeable, ObjectReceiver<InstancePacket>
{
	private T instance;
	private String instanceIdentifier;
	private InstancePacketConnection connection;
	
	public Reference(T instance, InstancePacketConnection connection) throws IOException
	{
		this.instanceIdentifier = instance.getClass().getCanonicalName();
		this.connection = connection;
		this.connection.receiveAsync(this, true);
			
		this.set(instance);
	}
	
	public T get()
	{
		if (this.connection.isAvailable())
		{
			return this.instance;
		}
		else
		{
			throw new IllegalStateException("Instance is out of sync: instance server is unavailable");
		}
	}
	
	public void set(T instance)
	{
		if (this.instance != null)
		{
			this.instance.deleteObserver(this);
		}
		this.instance = instance;
		this.instance.addObserver(this);
		
		this.setChanged();
		this.notifyObservers(this);
	}

	@Override
	public void notify(Observable<T> observable, T data)
	{
		if (this.connection.isAvailable())
		{
			try
			{
				this.connection.send(new InstanceSyncPacket(instance));
			}
			catch (IOException ex)
			{
				throw new RuntimeException("Instance out of sync: " + ex.getMessage());
			}
		}
		else
		{
			throw new IllegalStateException("Instance out of sync: unable to push update to instance server");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void receivePacket(InstancePacket packet, ObjectConnection<InstancePacket> connection) throws IOException
	{
		if (packet.getPacketType() == InstancePacketType.INSTANCE_SYNC && 
				packet.getInstanceIdentifier() == this.instanceIdentifier)
		{
			Log.v("Received instance update for " + packet.getInstanceIdentifier() + " from instance server");
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			try 
			{
				T instance = (T)syncPacket.getInstance().getObject();
				this.set(instance);
			} 
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}
	
	@Override
	public void close() throws IOException
	{
		connection.close();
	}
}
