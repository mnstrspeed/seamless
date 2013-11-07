package nl.tomsanders.seamless.packagemanager;

import java.io.IOException;
import java.net.Socket;

import nl.tomsanders.util.ObjectConnection;

public class PackageManagerConnection extends ObjectConnection<PackageManagerPacket> 
{
	public PackageManagerConnection(Socket socket) throws IOException 
	{
		super(socket);
	}
}
