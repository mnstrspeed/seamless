package nl.tomsanders.seamless.dsi.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.networking.InstancePacket;
import nl.tomsanders.seamless.networking.InstancePacketConnection;
import nl.tomsanders.seamless.networking.InstancePacketReceiver;
import nl.tomsanders.seamless.networking.InstancePacketType;
import nl.tomsanders.seamless.networking.InstanceSyncPacket;
import nl.tomsanders.seamless.networking.UnknownInstanceResponsePacket;

public class InstanceServer 
{
	protected static final String LOCAL_HOST = "127.0.0.1";
	protected static final int LOCAL_PORT = 1901;
	protected static final int EXTERNAL_PORT = 9501;
	
	private ServerSocket externalServerSocket;
	private ServerSocket internalServerSocket;
	
	private InternalInstancePacketReceiver internalReceiver;
	private ExternalInstancePacketReceiver externalReceiver;
	private InstanceServerDiscoveryService discoveryService;

	private Hashtable<String, InstanceSyncPacket> instances;
	private InstancePacketConnection internalConnection;
	private List<InstancePacketConnection> externalConnections;
	
	public InstanceServer()
	{
		this.externalConnections = new ArrayList<InstancePacketConnection>();
		this.instances = new Hashtable<String, InstanceSyncPacket>();
		
		this.internalReceiver = new InternalInstancePacketReceiver();
		this.externalReceiver = new ExternalInstancePacketReceiver();
	}
	
	public void start() throws IOException
	{
		// Start listening for internal connections
		this.internalServerSocket = new ServerSocket();
		this.internalServerSocket.bind(new InetSocketAddress(LOCAL_HOST, LOCAL_PORT));
		new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Log.v("Now listening for internal connections at " + LOCAL_HOST + ":" + LOCAL_PORT);
				while (!internalServerSocket.isClosed())
				{
					try
					{
						Socket connection = internalServerSocket.accept();
						Log.v("Incoming internal connection from " + connection.getInetAddress());
						
						handleInternalConnection(connection);
					}
					catch (Exception ex) 
					{ 
						Log.e("Exception was thrown while handling internal connection: " + ex);
					}
				}
			}	
		}).start();
		
		// Start listening for external connections
		this.externalServerSocket = new ServerSocket(EXTERNAL_PORT);
		new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Log.v("Now listening for external connections on port " + LOCAL_PORT);
				
				while (!externalServerSocket.isClosed())
				{
					try
					{
						Socket connection = externalServerSocket.accept();
						Log.v("Incoming external connection from " + connection.getInetAddress());
						
						handleExternalConnection(connection);
					}
					catch (Exception ex) 
					{ 
						Log.e("Exception was thrown while handling external connection: " + ex);
					}
				}
			}	
		}).start();
		
		this.discoveryService = new InstanceServerDiscoveryService(this);
		Log.v("Sending discovery broadcast on local network");
		this.discoveryService.sendBroadcast();
		Log.v("Listening for discovery broadcasts on local network");
		this.discoveryService.startListening();
	}
	
	public void registerNewInstanceServer(InetAddress address)
	{
		Log.v("Received discovery broadcast");
		
		try 
		{
			Socket socket = new Socket(address, EXTERNAL_PORT);
			InstancePacketConnection connection = new InstancePacketConnection(socket);
			
			this.sendAllInstances(connection);
			this.registerExternalConnection(connection);
		} 
		catch (Exception e)
		{
			Log.e("Failed to initiate connection with new InstanceServer: " +
					e.getMessage());
		}
	}
	
	protected void handleInternalConnection(Socket socket) throws IOException, ClassNotFoundException 
	{
		InstancePacketConnection connection = new InstancePacketConnection(socket);
		InstancePacket request = connection.receive();
		
		if (request.getPacketType() == InstancePacketType.INSTANCE_REQUEST)
		{
			if (!this.instances.containsKey(request.getInstanceIdentifier()))
			{
				// Should only happen on first time run
				Log.v(socket.getInetAddress() + " requested unknown instance " + request.getInstanceIdentifier());
				connection.send(new UnknownInstanceResponsePacket(request));
				
				// Client will now create a new instance and send it to
				// us in an InstanceSyncPacket
			}
			else
			{
				Log.v(socket.getInetAddress() + " requested instance " + request.getInstanceIdentifier());
				connection.send(this.instances.get(request.getInstanceIdentifier()));
			}
				
			// Listen for future updates
			this.internalConnection = connection;
			this.internalConnection.receiveAsync(this.internalReceiver, true);
		}
		else
		{
			throw new RuntimeException("Illegal request to server: connection may "
					+ "only be initiated with instance request");
		}
	}
	
	protected void handleExternalConnection(Socket socket) throws IOException, ClassNotFoundException
	{
		InstancePacketConnection connection = new InstancePacketConnection(socket);
		this.registerExternalConnection(connection);
	}

	private void registerExternalConnection(InstancePacketConnection connection) 
			throws IOException 
	{
		this.externalConnections.add(connection);
		connection.receiveAsync(this.externalReceiver, true);
	}
	
	private class InternalInstancePacketReceiver implements InstancePacketReceiver 
	{
		@Override
		public void receivePacket(InstancePacket packet,
				InstancePacketConnection connection) 
		{
			Log.v(connection.getSocket().getInetAddress() + 
				" updated " + packet.getInstanceIdentifier());
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			
			InstanceServer.this.instances.put(syncPacket.getInstanceIdentifier(), syncPacket);
			try
			{
				InstanceServer.this.updateExternalConnections(syncPacket.getInstanceIdentifier());
			}
			catch (IOException ex)
			{
				throw new RuntimeException("Invalid update from client: " + ex.getMessage());
			}
		}
	}
	
	private class ExternalInstancePacketReceiver implements InstancePacketReceiver
	{
		@Override
		public void receivePacket(InstancePacket packet,
				InstancePacketConnection connection) 
		{
			try
			{
				if (packet.getPacketType() == InstancePacketType.INSTANCE_REQUEST)
				{
					// Someone heard 'bout our instance and wants in!
					if (!InstanceServer.this.instances.containsKey(packet.getInstanceIdentifier()))
					{
						// Should only happen on first time run
						Log.v(connection.getSocket().getInetAddress() + " requested unknown instance " 
								+ packet.getInstanceIdentifier());
						connection.send(new UnknownInstanceResponsePacket(packet));
					}
					else
					{
						Log.v(connection.getSocket().getInetAddress() + " requested " 
								+ packet.getInstanceIdentifier());
						connection.send(InstanceServer.this.instances.get(packet.getInstanceIdentifier()));
					}
				}
				else if (packet.getPacketType() == InstancePacketType.INSTANCE_SYNC)
				{
					Log.v(connection.getSocket().getInetAddress() + 
							" updated " + packet.getInstanceIdentifier());
					
					InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
					InstanceServer.this.instances.put(syncPacket.getInstanceIdentifier(), syncPacket);
					try
					{
						InstanceServer.this.updateInternalConnection(syncPacket.getInstanceIdentifier());
						//InstanceServer.this.updateExternalConnections(connection);
					}
					catch (IOException ex)
					{
						throw new RuntimeException("Invalid update from client");
					}
				}
			}
			catch (Exception ex)
			{
				Log.e("Failed to process packet from " + connection.getSocket().getInetAddress() + ": " +
						ex.getMessage());
			}
		}
	}
	
	private boolean updateInternalConnection(String instanceIdentifier) throws IOException 
	{
		if (this.internalConnection != null && 
				!this.internalConnection.getSocket().isClosed())
		{
			this.internalConnection.send(this.instances.get(instanceIdentifier));
			return true;
		}
		else
		{
			return false;
		}
	}

	private void updateExternalConnections(String instanceIdentifier) throws IOException 
	{
		Iterator<InstancePacketConnection> iterator = this.externalConnections.iterator();
		while (iterator.hasNext())
		{
			InstancePacketConnection connection = iterator.next();
			if (!this.updateExternalConnection(connection, instanceIdentifier))
			{
				Log.w("Connection to " + connection.getSocket().getInetAddress() + " was lost");
				iterator.remove();
			}
		}
	}
	
	private boolean updateExternalConnection(InstancePacketConnection connection, String instanceIdentifier) 
			throws IOException
	{
		if (connection != null && 
				!connection.getSocket().isClosed())
		{
			if (this.instances.containsKey(instanceIdentifier))
			{
				Log.v("Sending " + instanceIdentifier + " to " + connection.getSocket().getInetAddress());
				connection.send(this.instances.get(instanceIdentifier));
			}
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private void sendAllInstances(InstancePacketConnection connection) 
			throws IOException
	{
		for (String instanceIdentifier : this.instances.keySet())
		{
			this.updateExternalConnection(connection, instanceIdentifier);
		}
	}

	public static void main(String[] args)
	{
		try 
		{	
			new InstanceServer().start();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
