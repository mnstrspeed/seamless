package nl.tomsanders.seamless.runtime;

import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.networking.ObjectConnection;
import nl.tomsanders.seamless.networking.InstancePacket;
import nl.tomsanders.seamless.networking.InstancePacketType;
import nl.tomsanders.seamless.networking.InstanceSyncPacket;

public class InstanceManagerPacketReceiver<T> implements ObjectConnection.ObjectReceiver<InstancePacket>
{
	private Reference<T> reference;
	
	public InstanceManagerPacketReceiver(Reference<T> reference)
	{
		this.reference = reference;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void receivePacket(InstancePacket packet, ObjectConnection<InstancePacket> connection) 
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
