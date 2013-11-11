package nl.tomsanders.seamless.packagemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import nl.tomsanders.seamless.dsi.logging.Log;
import nl.tomsanders.seamless.dsi.logging.LogLevel;
import nl.tomsanders.util.ObjectConnection;
import nl.tomsanders.util.ObjectReceiver;

public class PackageManager 
{
	private static final String PACKAGE_DIR = "packages";
	private static final String INDEX_FILE = "index.dat";
	private static final int EXTERNAL_PORT = 1812;

	private ArrayList<Package> packageIndex;
	private Hashtable<String, Process> runningProcesses;
	
	private PackageManagerDiscoveryService discoveryService;
	private ArrayList<PackageManagerConnection> connections;
	private ServerSocket socket;
	
	public PackageManager()
	{
		this.connections = new ArrayList<PackageManagerConnection>();
		this.runningProcesses = new Hashtable<String, Process>();
	}

	public void start() throws IOException
	{
		this.loadIndex();
		for (Package pack : this.packageIndex)
		{
			if (pack.isLatestVersion())
				launchPackage(pack);
		}
		
		this.socket = new ServerSocket(EXTERNAL_PORT);
		new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Log.v("Now listening for incoming connections");
				while (!socket.isClosed())
				{
					try
					{
						Socket connection = socket.accept();
						Log.v("Incoming connection from " + connection.getInetAddress());
						
						handleConnection(connection);
					}
					catch (Exception ex) 
					{ 
						Log.e("Exception was thrown while handling connection: " + ex);
					}
				}
			}	
		}).start();
		
		this.discoveryService = new PackageManagerDiscoveryService(this);
		Log.v("Sending discovery broadcast on local network");
		this.discoveryService.sendBroadcast();
		Log.v("Listening for discovery broadcasts on local network");
		this.discoveryService.startListening();
	}
	
	@SuppressWarnings("unchecked")
	private void loadIndex() throws FileNotFoundException, IOException 
	{
		File indexFile = new File(PACKAGE_DIR + "/" + INDEX_FILE);
		if (indexFile.isFile())
		{
			Log.v("Loading package index");
			try
			{
				ObjectInputStream deserializer = new ObjectInputStream(
						new FileInputStream(indexFile));
				this.packageIndex = (ArrayList<Package>)deserializer.readObject();
				deserializer.close();
			}
			catch (ClassNotFoundException ex)
			{
				throw new RuntimeException(ex);
			}
		}
		else
		{
			Log.v("No package index detected; creating new package index");
			this.packageIndex = new ArrayList<Package>();
			this.saveIndex();
		}
	}
	
	private void saveIndex() throws FileNotFoundException, IOException
	{
		Log.v("Saving package index to disk");
		ObjectOutputStream serializer = new ObjectOutputStream(
				new FileOutputStream(PACKAGE_DIR + "/" + INDEX_FILE));
		serializer.writeObject(this.packageIndex);
		serializer.close();
	}

	protected void handleConnection(Socket connection) throws IOException 
	{
		PackageManagerConnection c = new PackageManagerConnection(connection);
		this.connections.add(c);
		
		c.receiveAsync(new ObjectReceiver<PackageManagerPacket>() {
			@Override
			public void receivePacket(PackageManagerPacket packet,
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
					
					if (packageIndex.contains(requestPacket.getPackage()))
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
							Log.v("I NEED THIS");
							packagePacket.getPackage().setIsLatestVersion(true);
							for (Package other : packageIndex)
							{
								if (packagePacket.getPackage().getName() == other.getName())
									other.setIsLatestVersion(false);
							}
							
							packageIndex.add(packagePacket.getPackage());
							savePackage(packagePacket.getPackage(), packagePacket.getData());
							saveIndex();
							
							// Let the others know we now have a new package!
							Iterator<PackageManagerConnection> iterator = connections.iterator();
							while (iterator.hasNext())
							{
								PackageManagerConnection other = iterator.next();
								if (other != connection && other != null && other.isConnected())
								{
									other.send(new PackageIndexPacket(packageIndex));
								}
								if (other == connection)
								{
									// ignore
								}
								else
								{
									Log.v("Connection to " + other.getSocket().getInetAddress() + " was lost");
									iterator.remove();
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
		}, true);
	}
	
	protected void savePackage(Package pack, byte[] data) throws IOException
	{		
		FileOutputStream stream = new FileOutputStream(getPackagePath(pack));
		stream.write(data);
		stream.close();
	}

	private static String getPackagePath(Package pack)
	{
		return PACKAGE_DIR + "/" + pack.getName() + "-" + pack.getVersion() + ".jar";
	}
	
	private boolean packageWanted(Package offered)
	{
		for (Package p : this.packageIndex)
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

	public void registerNewPackageManager(InetAddress address) 
	{
		Log.v("Received package manager discovery broadcast; sending package index");
		
		try 
		{
			Socket socket = new Socket(address, EXTERNAL_PORT);
			PackageManagerConnection connection = new PackageManagerConnection(socket);
			
			connection.send(new PackageIndexPacket(this.packageIndex));
			this.connections.add(connection);
		} 
		catch (Exception e)
		{
			Log.e("Failed to initiate connection with new PackageManager: " +
					e.getMessage());
		}
	}
	
	public static void main(String[] args)
	{
		Log.LEVEL = LogLevel.VERBOSE;
		try
		{
			new PackageManager().start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
