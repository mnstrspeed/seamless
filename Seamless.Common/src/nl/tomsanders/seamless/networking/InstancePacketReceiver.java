package nl.tomsanders.seamless.networking;

public interface InstancePacketReceiver 
{
	public void receivePacket(InstancePacket packet, InstancePacketConnection connection);
}
