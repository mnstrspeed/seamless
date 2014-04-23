package nl.tomsanders.seamless.networking;

@SuppressWarnings("serial")
public class UnknownInstanceResponsePacket extends InstancePacket
{
	public UnknownInstanceResponsePacket(InstancePacket request) 
	{
		super(InstancePacketType.UNKNOWN_INSTANCE_RESPONSE, request);
	}
}
