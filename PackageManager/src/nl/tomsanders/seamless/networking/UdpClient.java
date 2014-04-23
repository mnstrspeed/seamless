package nl.tomsanders.seamless.networking;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClient implements Closeable
{	
	private final DatagramSocket socket;
	
	public UdpClient() throws SocketException
	{
		this.socket = new DatagramSocket();
	}
	
	public void multicast(byte[] data, final int port) 
			throws UnknownHostException, IOException
	{
		this.send(data, InetAddress.getByName("255.255.255.255"), port);
	}
	
	public void send(byte[] data, final InetAddress address, final int port) 
			throws IOException
	{
		DatagramPacket packet = new DatagramPacket(
				data, data.length, address, port);
		socket.send(packet);
	}
	
	@Override
	public void close()
	{
		this.socket.close();
	}
}
