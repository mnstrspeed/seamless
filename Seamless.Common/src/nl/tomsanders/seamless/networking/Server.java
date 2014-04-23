package nl.tomsanders.seamless.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import nl.tomsanders.seamless.logging.Log;

public class Server 
{
	public static interface ConnectionHandler
	{
		public void handle(Socket connection);
	}
	
	private final InetSocketAddress endpoint;
	private final ConnectionHandler handler;
	
	private ServerSocket serverSocket;
	private Thread thread;
	
	public Server(final InetSocketAddress endpoint, ConnectionHandler handler)
	{
		this.endpoint = endpoint;
		this.handler = handler;
	}
	
	public void start() throws IOException
	{
		this.serverSocket = new ServerSocket();
		this.serverSocket.bind(endpoint);
		
		this.thread = new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Log.v("Now listening for connections at " + endpoint.toString());
				while (!serverSocket.isClosed())
				{
					try
					{
						Socket connection = serverSocket.accept();
						handle(connection);
					}
					catch (Exception ex) 
					{ 
						Log.e("Exception was thrown while handling connection: " + ex);
					}
				}
			}
		});
		this.thread.start();
	}
	
	private void handle(Socket connection)
	{
		Log.v("Incoming connection from " + connection.getInetAddress());
		this.handler.handle(connection);
	}
}
