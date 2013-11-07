package nl.tomsanders.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpServer implements Closeable
{
	private final int port;
	private final DatagramSocket socket;
	
	public UdpServer(final int port) throws SocketException
	{
		this.port = port;
		this.socket = new DatagramSocket(this.port);
	}
	
	public DatagramPacket accept() throws IOException
	{
		byte[] receiveData = new byte[1024];
		DatagramPacket packet = new DatagramPacket(
				receiveData, receiveData.length);
		
		socket.receive(packet);
		return packet;
	}
	
	@Override
	public void close()
	{
		this.socket.close();
	}
}
