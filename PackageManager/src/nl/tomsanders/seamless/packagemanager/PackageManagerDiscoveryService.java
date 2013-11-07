package nl.tomsanders.seamless.packagemanager;

import java.net.DatagramPacket;
import java.net.SocketException;

import nl.tomsanders.util.UdpClient;
import nl.tomsanders.util.UdpServer;

public class PackageManagerDiscoveryService implements Runnable
{
	private static final int DISCOVERY_PORT = 1811;
	private static final byte[] DISCOVERY_MESSAGE = "Dave?".getBytes();
	
	private final PackageManager packageManager;
	
	private boolean running;
	private Thread serviceThread;
	
	private UdpServer multicastServer;
	private UdpClient multicastClient;
	
	public PackageManagerDiscoveryService(PackageManager packageManager) throws SocketException
	{
		this.packageManager = packageManager;
		
		this.running = false;
		this.multicastClient = new UdpClient();
	}
	
	public void sendBroadcast()
	{
		try 
		{
			this.multicastClient.multicast(DISCOVERY_MESSAGE, DISCOVERY_PORT);
		} 
		catch (Exception ex)
		{
			throw new RuntimeException("Unable to send discovery broadcast", ex);
		}
	}
	
	public void startListening()
	{
		this.serviceThread = new Thread(this);
		this.running = true;
		this.serviceThread.start();
	}
	
	public void stopListening()
	{
		this.running = false;
		this.serviceThread.interrupt();
	}
	
	@Override
	public void run() 
	{
		try
		{
			this.multicastServer = new UdpServer(DISCOVERY_PORT);
			while (this.running)
			{
				DatagramPacket packet = this.multicastServer.accept();
				this.packageManager.registerNewPackageManager(packet.getAddress());
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
}
