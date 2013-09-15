package nl.tomsanders.seamless.dsi.networking;

/**
 * Packet to request instance from the InstanceServer to initiate
 * the connection
 */
@SuppressWarnings("serial")
public class InstanceRequestPacket extends InstancePacket
{	
	public InstanceRequestPacket(Class<?> type)
	{
		super(InstancePacketType.INSTANCE_REQUEST, type);
	}
	
	public InstanceRequestPacket(Class<?> type, String identifier)
	{
		super(InstancePacketType.INSTANCE_REQUEST, type, identifier);
	}
}
