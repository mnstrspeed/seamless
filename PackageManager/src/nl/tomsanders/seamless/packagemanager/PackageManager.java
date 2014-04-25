package nl.tomsanders.seamless.packagemanager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import nl.tomsanders.seamless.config.NetworkConfiguration;
import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.networking.DiscoveryService;
import nl.tomsanders.seamless.networking.ObjectConnection;
import nl.tomsanders.seamless.networking.ObjectReceiver;
import nl.tomsanders.seamless.networking.Server;

public class PackageManager
{
	private PackageIndex packageIndex;
	
	private Server server;
	private DiscoveryService discoveryService;
	private ArrayList<PackageManagerConnection> connections;
	
	private Hashtable<String, Process> runningProcesses;
	
	public PackageManager()
	{
		this.connections = new ArrayList<PackageManagerConnection>();
		this.runningProcesses = new Hashtable<String, Process>();
	}

	public void start() throws IOException
	{
		this.packageIndex = PackageIndex.load();
		for (Package pack : this.packageIndex.getPackages())
		{
			if (pack.isLatestVersion())
				this.launchPackage(pack);
		}
		
		this.server = new Server(new InetSocketAddress(
				NetworkConfiguration.PACKAGEMANAGER_PORT),
				this::onHostConnected);
		this.server.setOnStartListener(e ->
			Log.v("Now listening for incoming connections at " + e));
		this.server.setExceptionHandler((ex, e) ->
			Log.e("Exception was thrown while handling incoming connection at " + e + ": " + ex));
		this.server.start();
		
		this.discoveryService = new DiscoveryService("Dave?", 
				NetworkConfiguration.PACKAGEMANAGER_DISCOVERY_PORT);
		Log.v("Waiting for network interface");
		this.discoveryService.waitForNetwork();
		Log.v("Sending discovery broadcast on local network");
		this.discoveryService.sendBroadcast();
		Log.v("Listening for discovery broadcasts on local network");
		this.discoveryService.startListening(this::onHostDiscovered);
	}
	
	public void onHostDiscovered(InetAddress address) 
	{
		Log.v("Received discovery broadcast from " + address + "; sending package index");
		
		try 
		{
			Socket socket = new Socket(address, NetworkConfiguration.PACKAGEMANAGER_PORT);
			PackageManagerConnection connection = new PackageManagerConnection(socket);
			
			connection.send(new PackageIndexPacket(this.packageIndex));
			this.connections.add(connection);
		} 
		catch (Exception e)
		{
			Log.e("Failed to initiate connection with PackageManager at " 
					+ address + ": " + e.getMessage());
		}
	}
	
	protected void onHostConnected(Socket connection) throws IOException 
	{
		PackageManagerConnection c = new PackageManagerConnection(connection);
		this.connections.add(c);
		
		c.receiveAsync(this::onPacketReceived, true);
	}
	
	public void onPacketReceived(PackageManagerPacket packet,
			ObjectConnection<PackageManagerPacket> connection)
	{
		if (packet.getType() == PacketManagerPacketType.INDEX)
		{
			Log.v("Received package index from " + connection.getSocket().getInetAddress());
			PackageIndexPacket indexPacket = (PackageIndexPacket)packet;
			for (Package p : indexPacket.getPackages())
			{
				if (packageWanted(p))
				{
					Log.v("Requesting " + p + " from " + connection.getSocket().getInetAddress());
					try
					{
						PackageRequestPacket request = new PackageRequestPacket(p);
						connection.send(request);
					}
					catch (IOException ex)
					{
						Log.e("Failed to send package request for " + p);
					}
				}
			}
		}
		if (packet.getType() == PacketManagerPacketType.REQUEST)
		{
			PackageRequestPacket requestPacket = (PackageRequestPacket)packet;
			Log.v("Received package request for " + requestPacket.getPackage() + " from " +
					connection.getSocket().getInetAddress());
			
			if (packageIndex.containsPackage(requestPacket.getPackage()))
			{
				try
				{
					PackagePacket response = new PackagePacket(requestPacket.getPackage(),
							Paths.get(getPackagePath(requestPacket.getPackage())));
					connection.send(response);
				}
				catch (IOException ex)
				{
					throw new RuntimeException(ex);
				}
			}
			else
			{
				Log.v("Unable to provide " + requestPacket.getPackage() + " for " +
					connection.getSocket().getInetAddress());
			}
		}
		if (packet.getType() == PacketManagerPacketType.PACKAGE)
		{
			PackagePacket packagePacket = (PackagePacket)packet;
			Log.v("Received package " + packagePacket.getPackage() + " from " +
					connection.getSocket().getInetAddress());
			
			try
			{
				if (packageWanted(packagePacket.getPackage()))
				{
					packagePacket.getPackage().setIsLatestVersion(true);
					for (Package other : packageIndex.getPackages())
					{
						if (packagePacket.getPackage().getName() == other.getName())
							other.setIsLatestVersion(false);
					}
					
					packageIndex.add(packagePacket.getPackage());
					savePackage(packagePacket.getPackage(), packagePacket.getData());
					packageIndex.save();
					
					// Let the others know we now have a new package!
					Iterator<PackageManagerConnection> iterator = connections.iterator();
					while (iterator.hasNext())
					{
						PackageManagerConnection other = iterator.next();
						if (other == null)
						{
							iterator.remove();
						}
						else if (other == connection) 
						{
							continue;
						}
						else if (other.isConnected())
						{
							other.send(new PackageIndexPacket(packageIndex));
						}
					}
					
					launchPackage(packagePacket.getPackage());
				}
				else
				{
					Log.v("Disregarding package " + packagePacket.getPackage() + " from " +
							connection.getSocket().getInetAddress());
				}
			}
			catch (Exception ex)
			{
				Log.e("Failed to update package " + packagePacket.getPackage() + ": " + ex.getMessage());
			}
		}
	}
	
	protected void savePackage(Package pack, byte[] data) throws IOException
	{		
		FileOutputStream stream = new FileOutputStream(getPackagePath(pack));
		stream.write(data);
		stream.close();
	}

	private static String getPackagePath(Package pack)
	{
		return PackageIndex.getPackageDir() + "/" + pack.getName() + "-" + pack.getVersion() + ".jar";
	}
	
	private boolean packageWanted(Package offered)
	{
		for (Package p : this.packageIndex.getPackages())
		{
			if (p.getName().equals(offered.getName()) && p.getVersion() >= offered.getVersion())
				return false;
		}
		return true;
	}
	
	private void launchPackage(Package pack)
	{
		Log.v("Launching package " + pack);
		if (this.runningProcesses.containsKey(pack.getName()))
		{
			this.runningProcesses.get(pack.getName()).destroy();
		}
		
		String path = getPackagePath(pack);
		try 
		{
			Process p = Runtime.getRuntime().exec("java -jar " + path);
			this.runningProcesses.put(pack.getName(), p);
		} 
		catch (IOException e) 
		{
			Log.e("Unable to launch package " + pack);
		}
	}
}
