package nl.tomsanders.seamless.dsi.networking;

import java.io.Serializable;

/**
 * Abstract base type for packets. Contains package name and packet type.
 */
@SuppressWarnings("serial")
public abstract class InstancePacket implements Serializable
{
	private final InstancePacketType packetType;
	private final String instanceIdentifier;
	
	protected InstancePacket(InstancePacketType packetType, Class<?> type)
	{
		this.packetType = packetType;
		this.instanceIdentifier = type.getCanonicalName();
	}
	
	protected InstancePacket(InstancePacketType packetType, Class<?> type, String identifier)
	{
		this.packetType = packetType;
		this.instanceIdentifier = type.getCanonicalName() + "#" + identifier;
	}
	
	public InstancePacket(InstancePacketType packetType, InstancePacket request) {
		this.packetType = packetType;
		this.instanceIdentifier = request.getInstanceIdentifier();
	}

	public InstancePacketType getPacketType()
	{
		return this.packetType;
	}
	
	public String getInstanceIdentifier()
	{
		return this.instanceIdentifier;
	}
}
