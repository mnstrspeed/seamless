package nl.tomsanders.seamless.dsi;

import nl.tomsanders.seamless.dsi.logging.Log;
import nl.tomsanders.seamless.dsi.networking.InstancePacket;
import nl.tomsanders.seamless.dsi.networking.InstancePacketConnection;
import nl.tomsanders.seamless.dsi.networking.InstancePacketReceiver;
import nl.tomsanders.seamless.dsi.networking.InstancePacketType;
import nl.tomsanders.seamless.dsi.networking.InstanceSyncPacket;

public class InstanceManagerPacketReceiver<T> implements InstancePacketReceiver
{
	private Reference<T> reference;
	
	public InstanceManagerPacketReceiver(Reference<T> reference)
	{
		this.reference = reference;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receivePacket(InstancePacket packet, InstancePacketConnection connection) 
	{
		if (packet.getPacketType() == InstancePacketType.INSTANCE_SYNC)
		{
			Log.v("Received instance update for " + packet.getInstanceIdentifier() + " from instance server");
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			try 
			{
				T instance = (T)syncPacket.getInstance().getObject();
				this.reference.setModel(instance);
			} 
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
		else
		{
			Log.v("Received unknown packet from instance server");
		}
	}
}
