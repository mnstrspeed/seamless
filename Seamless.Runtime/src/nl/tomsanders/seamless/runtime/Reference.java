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
import nl.tomsanders.seamless.util.Mergable;
import nl.tomsanders.seamless.util.Observable;
import nl.tomsanders.seamless.util.Observer;

/**
 * Reference to a shared instance. The Reference will ensure the
 * shared instance will remain synchronized across the network by
 * observing the instance through the Observer interface and
 * updating the reference when it is changed on another host.
 */
public class Reference<T extends Observable<T> & Mergable<T> & Serializable> 
		extends Observable<T> implements Observer<T>, Closeable, ObjectReceiver<InstancePacket>
{
	private T instance;
	private String instanceIdentifier;
	private InstancePacketConnection connection;
	
	/**
	 * Create a reference to a shared instance after it is initialized by
	 * the Runtime
	 */
	public Reference(T instance, String instanceIdentifier, InstancePacketConnection connection) throws IOException
	{
		this.instanceIdentifier = instanceIdentifier;
		
		this.connection = connection;
		this.connection.receiveAsync(this, true);
			
		this.updateReference(instance);
	}
	
	/**
	 * Get the current version of the shared instance. Note that the Object
	 * only reflects the current state, and will be out of sync as soon as
	 * a newer version is received. Therefore, it is important to always store
	 * the Reference instead of the instance obtained with this method.
	 */
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
	
	
	/**
	 * Set a new version of the shared instance. This update will be synchronized
	 * with other hosts.
	 */
	public void set(T instance)
	{
		this.updateReference(instance);
		
		this.setChanged();
		this.notifyObservers(instance);
	}
	
	/**
	 * Update the instance reference. This method ensures that all observer connections
	 * are updated appropriately.
	 */
	private void updateReference(T instance)
	{
		if (this.instance != null)
		{
			this.instance.deleteObserver(this);
		}
		this.instance = instance;
		this.instance.addObserver(this);
	}

	@Override
	public void notify(Observable<T> observable, T data)
	{
		// Called whenever the instance has been updated, 
		// send a new version to the InstanceServer
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
		
		this.setChanged();
		this.notifyObservers(instance);
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
				T update = (T)syncPacket.getInstance().getObject();
				
				// Merge the current instance with the updated instance
				T instance = this.instance.merge(update);
				this.updateReference(instance);
			} 
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}
	
	/**
	 * Releases all connections and their associated threads made by
	 * this Reference. This will ensure that the program can terminate.
	 */
	@Override
	public void close() throws IOException
	{
		connection.close();
	}
}
