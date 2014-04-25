package nl.tomsanders.seamless.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class Server 
{	
	public static interface ConnectionHandler
	{
		public void handle(Socket connection) throws Exception;
	}
	
	public static interface ExceptionHandler
	{
		public void handle(Exception ex, SocketAddress endpoint);
	}
	
	public static interface EventListener
	{
		public void notify(SocketAddress endpoint);
	}
	
	private final SocketAddress endpoint;
	private final ConnectionHandler connectionHandler;
	
	private ServerSocket serverSocket;
	private Thread thread;
	
	private ExceptionHandler exceptionHandler;
	private EventListener onStartListener;
	
	public Server(final SocketAddress endpoint, final ConnectionHandler handler)
	{
		this.endpoint = endpoint;
		this.connectionHandler = handler;
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
				onStart();
				while (!serverSocket.isClosed())
				{
					try
					{
						Socket connection = serverSocket.accept();
						handle(connection);
					}
					catch (Exception ex) 
					{ 
						onException(ex);
					}
				}
			}
		});
		this.thread.start();
	}
	
	public void setOnStartListener(EventListener listener)
	{
		this.onStartListener = listener;
	}
	
	public void setExceptionHandler(ExceptionHandler handler)
	{
		this.exceptionHandler = handler;
	}
	
	private void handle(Socket connection) throws Exception
	{
		this.connectionHandler.handle(connection);
	}
	
	private void onException(Exception ex) 
	{
		if (this.exceptionHandler != null)
			this.exceptionHandler.handle(ex, this.endpoint);
	}

	private void onStart() 
	{
		if (this.onStartListener != null)
			this.onStartListener.notify(this.endpoint);
	}
}
