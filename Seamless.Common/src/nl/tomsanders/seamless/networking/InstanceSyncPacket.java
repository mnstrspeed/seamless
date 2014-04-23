package nl.tomsanders.seamless.networking;

import java.io.IOException;
import java.io.Serializable;

/**
 * An instance packet used to synchronize instances, that is, an incoming
 * updated instance from another host.
 */
@SuppressWarnings("serial")
public class InstanceSyncPacket extends InstancePacket 
{
	private final long time;
	private final InstanceData instance;
	
	public InstanceSyncPacket(Serializable instance) throws IOException
	{
		super(InstancePacketType.INSTANCE_SYNC, instance.getClass());
		
		this.time = System.currentTimeMillis();
		this.instance = new InstanceData(instance);
	}
	
	public InstanceSyncPacket(Serializable instance, String identifier) throws IOException
	{
		super(InstancePacketType.INSTANCE_SYNC, instance.getClass(), identifier);
		
		this.time = System.currentTimeMillis();
		this.instance = new InstanceData(instance);
	}
	
	public InstanceSyncPacket(Class<?> type, long time, InstanceData instance)
	{
		super(InstancePacketType.INSTANCE_SYNC, type);
		
		this.time= time;
		this.instance = instance;
	}
	
	public InstanceSyncPacket(Class<?> type, String identifier, long time, InstanceData instance)
	{
		super(InstancePacketType.INSTANCE_SYNC, type, identifier);
		
		this.time= time;
		this.instance = instance;
	}
	
	public long getTime()
	{
		return this.time;
	}
	
	public InstanceData getInstance()
	{
		return this.instance;
	}
}
