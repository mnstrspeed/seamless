package nl.tomsanders.seamless.dsi.networking;

/**
 * Packet to request instance from the InstanceServer to initiate
 * the connection
 */
@SuppressWarnings("serial")
public class InstanceRequestPacket extends InstancePacket
{
	public InstanceRequestPacket(String packageName) 
	{
		super(InstancePacketType.INSTANCE_REQUEST, packageName);
	}
	
	public InstanceRequestPacket(Class<?> type)
	{
		super(InstancePacketType.INSTANCE_REQUEST, type);
	}
}
