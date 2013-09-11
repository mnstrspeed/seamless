package nl.tomsanders.seamless.dsi.networking;

import java.io.Serializable;

/**
 * Abstract base type for packets. Contains package name and packet type.
 */
@SuppressWarnings("serial")
public abstract class InstancePacket implements Serializable
{
	private final InstancePacketType packetType;
	private final String packageName;
	
	protected InstancePacket(InstancePacketType packetType, String packageName)
	{
		this.packetType = packetType;
		this.packageName = packageName;
	}
	
	protected InstancePacket(InstancePacketType packetType, Class<?> type)
	{
		this.packetType = packetType;
		this.packageName = type.getPackage().getName();
	}
	
	public InstancePacketType getPacketType()
	{
		return this.packetType;
	}
	
	public String getPackageName()
	{
		return this.packageName;
	}
}
