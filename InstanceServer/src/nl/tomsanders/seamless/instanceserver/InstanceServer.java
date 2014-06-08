package nl.tomsanders.seamless.instanceserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import nl.tomsanders.seamless.config.NetworkConfiguration;
import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.networking.DiscoveryService;
import nl.tomsanders.seamless.networking.InstancePacket;
import nl.tomsanders.seamless.networking.InstancePacketConnection;
import nl.tomsanders.seamless.networking.InstancePacketType;
import nl.tomsanders.seamless.networking.InstanceSyncPacket;
import nl.tomsanders.seamless.networking.ObjectConnection;
import nl.tomsanders.seamless.networking.Server;
import nl.tomsanders.seamless.networking.UnknownInstanceResponsePacket;

/**
 * The InstanceServer runs on each device. It maintains a list of
 * running instances. The Runtime can request these instances and
 * any updates of an instance will be synchronized across the network.
 *
 */
public class InstanceServer
{
	private Server localServer;
	private Server remoteServer;
	private DiscoveryService discoveryService;

	private List<InstancePacketConnection> localConnections;
	private List<InstancePacketConnection> remoteConnections;
	private Hashtable<String, InstanceSyncPacket> instances;
	
	public InstanceServer()
	{
		this.remoteConnections = new ArrayList<InstancePacketConnection>();
		this.localConnections = new ArrayList<InstancePacketConnection>();
		
		this.instances = new Hashtable<String, InstanceSyncPacket>();
		this.loadInstances();
	}

	@SuppressWarnings("unchecked")
	private void loadInstances()
	{
		boolean loaded = false;
		if (new File("instances").isFile())
		{
			try (ObjectInputStream input = new ObjectInputStream(new FileInputStream("instances")))
			{
				this.instances = (Hashtable<String, InstanceSyncPacket>)input.readObject();
				loaded = true;
			}
			catch (IOException | ClassNotFoundException e)
			{
				Log.e("Failed to load instances from disk: " + e.getMessage());
			}
		}
		
		if (!loaded)
		{
			Log.v("Creating new instance file on disk");
			this.instances = new Hashtable<String, InstanceSyncPacket>();
			this.saveInstances();
		}
	}
	
	private void saveInstances()
	{
		try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream("instances", false)))
		{
			output.writeObject(this.instances);
		}
		catch (IOException e)
		{
			Log.e("Unable to write instances to disk: " + e.getMessage());
		}
	}

	public void start() throws IOException
	{
		// Start listening for local connections
		this.localServer = new Server(new InetSocketAddress(
				NetworkConfiguration.LOCAL_HOST, 
				NetworkConfiguration.INSTANCESERVER_LOCAL_PORT),
				this::onLocalConnection);
		this.localServer.setOnStartListener(endpoint ->
			Log.v("Now listening for local connections at " + endpoint));
		this.localServer.setExceptionHandler((ex, endpoint) ->
			Log.e("Exception while handling local connection at " + endpoint + ": " + ex));
		this.localServer.start();
		
		// Start listening for remote connections
		this.remoteServer = new Server(new InetSocketAddress(
				NetworkConfiguration.INSTANCESERVER_REMOTE_PORT),
				this::onRemoteConnection);
		this.remoteServer.setOnStartListener(endpoint ->
			Log.v("Now listening for remote connections at " + endpoint));
		this.remoteServer.setExceptionHandler((ex, endpoint) ->
			Log.e("Exception while handling remote connection at " + endpoint + ": " + ex));
		this.remoteServer.start();
		
		// Start discovery service
		this.discoveryService = new DiscoveryService("Dave?", 
				NetworkConfiguration.INSTANCESERVER_DISCOVERY_PORT);
		Log.v("Waiting for network interface");
		this.discoveryService.waitForNetwork();
		Log.v("Sending discovery broadcast on local network");
		this.discoveryService.sendBroadcast();
		Log.v("Listening for discovery broadcasts on local network port " + 
				NetworkConfiguration.INSTANCESERVER_DISCOVERY_PORT);
		this.discoveryService.startListening(this::onHostDiscovered);
	}
	
	public void onHostDiscovered(InetAddress address)
	{
		Log.v("Received discovery broadcast from " + address.getHostAddress());
		
		try
		{
			Socket socket = new Socket(address, NetworkConfiguration.INSTANCESERVER_REMOTE_PORT);
			InstancePacketConnection connection = new InstancePacketConnection(socket);
			
			this.addRemoteConnection(connection);
			
			// Send all instances
			for (InstanceSyncPacket instance : this.instances.values())
			{
				this.updateConnection(connection, instance);
			}
		} 
		catch (Exception e)
		{
			Log.e("Failed to initiate connection with new host: " + e.getMessage());
		}
	}
	
	protected void onLocalConnection(Socket socket) throws IOException, ClassNotFoundException 
	{
		InstancePacketConnection connection = new InstancePacketConnection(socket);
		InstancePacket request = connection.receive();
		
		if (request.getPacketType() == InstancePacketType.INSTANCE_REQUEST)
		{
			if (this.instances.containsKey(request.getInstanceIdentifier()))
			{
				Log.v(socket.getInetAddress() + " requested instance " + request.getInstanceIdentifier());
				connection.send(this.instances.get(request.getInstanceIdentifier()));
			}
			else
			{
				// Should only happen on first time run
				Log.v(socket.getInetAddress() + " requested unknown instance " + request.getInstanceIdentifier());
				connection.send(new UnknownInstanceResponsePacket(request));
				
				// Client will now create a new instance and send it to
				// us in an InstanceSyncPacket
			}
			
			this.addLocalConnection(connection);
		}
		else
		{
			connection.close();
			Log.e("Illegal request to server: connection may "
					+ "only be initiated with instance request");
		}
	}
	
	protected void onRemoteConnection(Socket socket) throws IOException, ClassNotFoundException
	{
		Log.v("Incoming remote connection from " + socket.getInetAddress());
		this.addRemoteConnection(new InstancePacketConnection(socket));
	}
	
	private void addLocalConnection(InstancePacketConnection connection) throws IOException 
	{
		Log.v("Incoming local connection");
		this.localConnections.add(connection);
		connection.receiveAsync(this::onLocalPacket, true);
	}

	private void addRemoteConnection(InstancePacketConnection connection) throws IOException 
	{
		this.remoteConnections.add(connection);
		connection.receiveAsync(this::onRemotePacket, true);
	}
	
	public void onLocalPacket(InstancePacket packet, ObjectConnection<InstancePacket> connection) 
	{
		if (packet.getPacketType() == InstancePacketType.INSTANCE_SYNC)
		{
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			Log.v(connection + " updated " + packet.getInstanceIdentifier());
			
			this.instances.put(syncPacket.getInstanceIdentifier(), syncPacket);
			this.updateConnections(this.remoteConnections, syncPacket);
			this.saveInstances();
		}
		else
		{
			Log.e("Illegal request to server by connection " + connection + 
					": " + packet.getPacketType());
		}
	}
	
	public void onRemotePacket(InstancePacket packet, ObjectConnection<InstancePacket> connection) throws IOException 
	{
		if (packet.getPacketType() == InstancePacketType.INSTANCE_REQUEST)
		{
			if (!this.instances.containsKey(packet.getInstanceIdentifier()))
			{
				Log.v(connection + " requested unknown instance " + packet.getInstanceIdentifier());
				connection.send(new UnknownInstanceResponsePacket(packet));
			}
			else
			{
				Log.v(connection + " requested " + packet.getInstanceIdentifier());
				connection.send(this.instances.get(packet.getInstanceIdentifier()));
			}
		}
		else if (packet.getPacketType() == InstancePacketType.INSTANCE_SYNC)
		{
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			Log.v(connection + " updated " + packet.getInstanceIdentifier());
			
			this.instances.put(syncPacket.getInstanceIdentifier(), syncPacket);
			
			this.updateConnections(this.localConnections, syncPacket);
			this.saveInstances();
			//this.updateConnections(remoteConnections.filter(
			//		c -> c != connection, packet); // act as hub?
		}
		else
		{
			Log.e("Illegal request to server by connection " + connection + 
					": " + packet.getPacketType());
		}
	}
	
	private void updateConnections(List<InstancePacketConnection> connections, 
			InstanceSyncPacket instance)
	{
		for (Iterator<InstancePacketConnection> iterator = connections.iterator(); iterator.hasNext(); )
		{
			InstancePacketConnection connection = iterator.next();
			try
			{
				this.updateConnection(connection, instance);
			}
			catch (IOException ex)
			{
				iterator.remove();
			}
		}
	}
	
	private void updateConnection(InstancePacketConnection connection, 
			InstanceSyncPacket instance) throws IOException
	{
		if (connection != null && connection.isAvailable())
		{
			Log.v("Sending " + instance.getInstanceIdentifier() + " to " + connection);
			connection.send(instance);
		}
		else
		{
			throw new IOException("Connection closed");
		}
	}
}
