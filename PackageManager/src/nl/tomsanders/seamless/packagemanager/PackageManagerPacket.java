package nl.tomsanders.seamless.packagemanager;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PackageManagerPacket implements Serializable
{
	private PacketManagerPacketType type;
	
	public PackageManagerPacket(PacketManagerPacketType type)
	{
		this.type = type;
	}
	
	public PacketManagerPacketType getType()
	{
		return this.type;
	}
}
