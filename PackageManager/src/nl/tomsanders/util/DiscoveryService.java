package nl.tomsanders.util;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class DiscoveryService implements Runnable {
	public static interface DiscoveryServiceListener {
		public void onHostDiscovered(InetAddress address);
	}
	
	private final int discoveryPort;
	private final byte[] discoveryMessage;
	
	private boolean running;
	private Thread serviceThread;
	private DiscoveryServiceListener listener;
	
	private UdpServer multicastServer;
	private UdpClient multicastClient;
	
	public DiscoveryService(final String tag, final int port) throws SocketException
	{	
		this.discoveryMessage = tag.getBytes();
		this.discoveryPort = port;
		
		this.running = false;
		this.multicastClient = new UdpClient();
	}
	
	public void waitForNetwork()
	{
		try
		{
			boolean multicastInterfaceUp = false;
			while (!multicastInterfaceUp)
			{
				for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
				{
					if (iface.isUp() && iface.supportsMulticast())
					{
						multicastInterfaceUp = true;
						break;
					}
				}
				
				if (!multicastInterfaceUp)
				{
					Thread.sleep(100);
				}
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException("Exception while waiting for network", ex);
		}
	}
	
	public void sendBroadcast()
	{
		try 
		{
			this.multicastClient.multicast(discoveryMessage, discoveryPort);
		} 
		catch (Exception ex)
		{
			// TODO: we probably want to keep sending this until it's gone
			// through (network might take a while to be up)
			throw new RuntimeException("Unable to send discovery broadcast", ex);
		}
	}
	
	public void startListening(DiscoveryServiceListener listener)
	{
		this.listener = listener;
		
		this.running = true;
		this.serviceThread = new Thread(this);
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
			this.multicastServer = new UdpServer(discoveryPort);
			while (this.running)
			{
				DatagramPacket packet = this.multicastServer.accept();
				this.listener.onHostDiscovered(packet.getAddress());
				
				// TODO: untested
				if (Thread.interrupted()) {
					this.running = false;
				}
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
