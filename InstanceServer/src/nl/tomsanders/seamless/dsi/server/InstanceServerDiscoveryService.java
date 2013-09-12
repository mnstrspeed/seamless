package nl.tomsanders.seamless.dsi.server;

import java.net.DatagramPacket;
import java.net.SocketException;

import nl.tomsanders.util.UdpClient;
import nl.tomsanders.util.UdpServer;

public class InstanceServerDiscoveryService implements Runnable
{
	private static final int DISCOVERY_PORT = 1809;
	private static final byte[] DISCOVERY_MESSAGE = "Dave?".getBytes();
	
	private final InstanceServer instanceServer;
	
	private boolean running;
	private Thread serviceThread;
	
	private UdpServer multicastServer;
	private UdpClient multicastClient;
	
	public InstanceServerDiscoveryService(InstanceServer server) throws SocketException
	{
		this.instanceServer = server;
		
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
				this.instanceServer.registerNewInstanceServer(packet.getAddress());
				
				// accept & receive
				// -> notify InstanceServer of new discovery
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
}
