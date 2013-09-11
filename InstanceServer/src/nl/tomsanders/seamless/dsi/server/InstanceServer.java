package nl.tomsanders.seamless.dsi.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
	
	// For single instance
	private InstanceSyncPacket instance;
	private InstancePacketConnection internalConnection;
	private List<InstancePacketConnection> externalConnections;
	
	public InstanceServer()
	{
		this.externalConnections = new ArrayList<InstancePacketConnection>();
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
		
		// Start listening for external connections
		this.externalServerSocket = new ServerSocket(EXTERNAL_PORT);
		new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Log.v("Now listening for external connections at port " + LOCAL_PORT);
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
		
		// TODO: Broadcast running instances? Other instance servers HAVE to know
		// what's running on the network
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
			}
			else
			{
				Log.v(socket.getInetAddress() + " requested instance " + request.getPackageName());
				connection.send(this.instance);
			}
				
			// Listen for future updates
			this.internalConnection = connection;
			this.internalConnection.receiveAsync(new InstancePacketReceiver()
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
			}, true);
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
			Log.v("Received instance update from " + connection.getSocket().getInetAddress() + 
					" for " + request.getPackageName());
			InstanceSyncPacket syncPacket = (InstanceSyncPacket)request;
			
			this.instance = syncPacket;
			try
			{
				this.updateExternalConnections();
			}
			catch (IOException ex)
			{
				throw new RuntimeException("Invalid update from client");
			}
		}
		
		connection.receiveAsync(new InstancePacketReceiver()
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
					InstanceServer.this.updateExternalConnections(connection);
				}
				catch (IOException ex)
				{
					throw new RuntimeException("Invalid update from client");
				}
			}
		}, true);
		this.externalConnections.add(connection);
	}
	
	private void updateExternalConnections() throws IOException 
	{
		for (InstancePacketConnection connection : this.externalConnections)
		{
			connection.send(this.instance);
		}
	}
	
	private void updateExternalConnections(InstancePacketConnection source) throws IOException 
	{
		for (InstancePacketConnection connection : this.externalConnections)
		{
			if (connection != source)
			{
				connection.send(this.instance);
			}
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
