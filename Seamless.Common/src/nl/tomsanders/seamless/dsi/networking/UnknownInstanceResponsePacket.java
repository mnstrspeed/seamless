package nl.tomsanders.seamless.dsi.networking;

@SuppressWarnings("serial")
public class UnknownInstanceResponsePacket extends InstancePacket
{
	public UnknownInstanceResponsePacket(InstancePacket request) 
	{
		super(InstancePacketType.UNKNOWN_INSTANCE_RESPONSE, request);
	}
}
