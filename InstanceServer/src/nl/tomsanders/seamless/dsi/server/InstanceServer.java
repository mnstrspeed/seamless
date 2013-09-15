package nl.tomsanders.seamless.dsi.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.tomsanders.seamless.dsi.logging.Log;
import nl.tomsanders.seamless.dsi.networking.InstancePacket;
import nl.tomsanders.seamless.dsi.networking.InstancePacketConnection;
import nl.tomsanders.seamless.dsi.networking.InstancePacketReceiver;
import nl.tomsanders.seamless.dsi.networking.InstancePacketType;
import nl.tomsanders.seamless.dsi.networking.InstanceSyncPacket;
import nl.tomsanders.seamless.dsi.networking.UnknownInstanceResponsePacket;

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
	
	// For single instance
	private InstanceSyncPacket instance;
	private InstancePacketConnection internalConnection;
	private List<InstancePacketConnection> externalConnections;
	
	public InstanceServer()
	{
		this.externalConnections = new ArrayList<InstancePacketConnection>();
		
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
						Log.v("Incoming internal connection from " + connection.getInetAddress() + " was handled");
					}
					catch (Exception ex) 
					{ 
						Log.e("Exception was thrown while handling internal connection: " + ex);
					}
				}
			}	
		}).start();
		
		this.discoveryService = new InstanceServerDiscoveryService(this);
		
		// Start listening for external connections
		this.externalServerSocket = new ServerSocket(EXTERNAL_PORT);
		new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Log.v("Now listening for external connections on port " + LOCAL_PORT);
				Log.v("Sending discovery broadcast on local network");
				InstanceServer.this.discoveryService.sendBroadcast();
				
				while (!externalServerSocket.isClosed())
				{
					try
					{
						Socket connection = externalServerSocket.accept();
						Log.v("Incoming external connection from " + connection.getInetAddress());
						
						handleExternalConnection(connection);
						Log.v("Incoming external connection from " + connection.getInetAddress() + " was handled");
					}
					catch (Exception ex) 
					{ 
						Log.e("Exception was thrown while handling internal connection: " + ex);
					}
				}
			}	
		}).start();
		
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
			
			this.updateExternalConnection(connection);
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
			if (this.instance == null)
			{
				// Should only happen on first time run
				Log.v(socket.getInetAddress() + " requested unknown instance " + request.getPackageName());
				connection.send(new UnknownInstanceResponsePacket(request));
				
				// Client will now create a new instance and send it to
				// us in an InstanceSyncPacket
			}
			else
			{
				Log.v(socket.getInetAddress() + " requested instance " + request.getPackageName());
				connection.send(this.instance);
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
		InstancePacket request = connection.receive();
		
		if (request.getPacketType() == InstancePacketType.INSTANCE_REQUEST)
		{
			// Someone heard 'bout our instance and wants in!
			if (this.instance == null)
			{
				// Should only happen on first time run
				Log.v(socket.getInetAddress() + " requested unknown instance " + request.getPackageName());
				connection.send(new UnknownInstanceResponsePacket(request));
			}
			else
			{
				Log.v(socket.getInetAddress() + " requested instance " + request.getPackageName());
				connection.send(this.instance);
			}
		}
		else if (request.getPacketType() == InstancePacketType.INSTANCE_SYNC)
		{
			this.externalReceiver.receivePacket(request, connection);
		}
		
		this.registerExternalConnection(connection);
	}

	private void registerExternalConnection(InstancePacketConnection connection) 
			throws IOException 
	{
		connection.receiveAsync(this.externalReceiver, true);
		this.externalConnections.add(connection);
	}
	
	private class InternalInstancePacketReceiver implements InstancePacketReceiver 
	{
		@Override
		public void receivePacket(InstancePacket packet,
				InstancePacketConnection connection) 
		{
			Log.v("Received instance update from " + connection.getSocket().getInetAddress() + 
				" for " + packet.getPackageName());
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			
			InstanceServer.this.instance = syncPacket;
			try
			{
				InstanceServer.this.updateExternalConnections();
			}
			catch (IOException ex)
			{
				throw new RuntimeException("Invalid update from client");
			}
		}
	}
	
	private class ExternalInstancePacketReceiver implements InstancePacketReceiver
	{
		@Override
		public void receivePacket(InstancePacket packet,
				InstancePacketConnection connection) 
		{
			Log.v("Received instance update from " + connection.getSocket().getInetAddress() + 
				" for " + packet.getPackageName());
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)packet;
			
			InstanceServer.this.instance = syncPacket;
			try
			{
				InstanceServer.this.updateInternalConnection();
				//InstanceServer.this.updateExternalConnections(connection);
			}
			catch (IOException ex)
			{
				throw new RuntimeException("Invalid update from client");
			}
		}
	}
	
	private boolean updateInternalConnection() throws IOException 
	{
		if (this.internalConnection != null && 
				!this.internalConnection.getSocket().isClosed())
		{
			this.internalConnection.send(this.instance);
			return true;
		}
		else
		{
			return false;
		}
	}

	private void updateExternalConnections() throws IOException 
	{
		Iterator<InstancePacketConnection> iterator = this.externalConnections.iterator();
		while (iterator.hasNext())
		{
			InstancePacketConnection connection = iterator.next();
			if (!this.updateExternalConnection(connection))
			{
				Log.w("Connection to " + connection.getSocket().getInetAddress() + " was lost");
				iterator.remove();
			}
		}
	}
	
	private boolean updateExternalConnection(InstancePacketConnection connection) 
			throws IOException
	{
		if (connection != null && 
				!connection.getSocket().isClosed())
		{
			if (this.instance != null)
			{
				connection.send(this.instance);
			}
			return true;
		}
		else
		{
			return false;
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
