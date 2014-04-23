package nl.tomsanders.seamless.dsi.networking;

public interface InstancePacketReceiver 
{
	public void receivePacket(InstancePacket packet, InstancePacketConnection connection);
}
