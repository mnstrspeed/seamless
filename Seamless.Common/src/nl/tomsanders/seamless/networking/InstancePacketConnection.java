package nl.tomsanders.seamless.networking;

import java.io.IOException;
import java.net.Socket;

public class InstancePacketConnection extends ObjectConnection<InstancePacket>
{
	public InstancePacketConnection(Socket socket) throws IOException
	{
		super(socket);
	}

}