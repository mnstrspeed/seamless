package nl.tomsanders.seamless.networking;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import nl.tomsanders.seamless.logging.Log;

public class InstancePacketConnection implements Closeable
{
	public static interface InstancePacketReceiver 
	{
		public void receivePacket(InstancePacket packet, InstancePacketConnection connection) throws IOException;
	}
	
	private final Socket connection;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	private boolean isReceivingAsync = false;
	private Thread asyncThread;
	private boolean available;
	
	public InstancePacketConnection(Socket socket) throws IOException
	{
		this.connection = socket;
		this.outputStream = new ObjectOutputStream(this.connection.getOutputStream());
		this.inputStream = new ObjectInputStream(this.connection.getInputStream()); 
		// Blocked until remote ObjectOutputStream is initialized
		
		this.available = true;
	}
	
	public void send(InstancePacket packet) throws IOException
	{
		try
		{
			this.outputStream.writeObject(packet);
			this.outputStream.flush();
		}
		catch (IOException ex)
		{
			this.available = false;
			throw ex;
		}
	}
	
	public InstancePacket receive() throws ClassNotFoundException, IOException
	{
		if (this.isReceivingAsync)
		{
			throw new IllegalStateException("Blocked receiving is not available "
					+ "while receiving asynchronously");
		}
		
		try
		{
			return (InstancePacket)this.inputStream.readObject();
		}
		catch (IOException ex)
		{
			Log.e("Connection to " + connection.getInetAddress() + " interrupted");
			this.available = false;
			
			throw ex;
		}
	}
	
	public void receiveAsync(InstancePacketReceiver receiver, boolean repeat) throws IOException
	{
		this.isReceivingAsync = true;
		
		this.asyncThread = new Thread(new AsyncReceiver(this.inputStream, receiver, repeat));
		this.asyncThread.start();
	}
	
	public void close() throws IOException
	{
		this.connection.close();
		this.available = false;
	}
	
	public Socket getSocket()
	{
		return this.connection;
	}
	
	public boolean isAvailable()
	{
		return this.available;
	}
	
	private class AsyncReceiver implements Runnable
	{
		private ObjectInputStream stream;
		private InstancePacketReceiver receiver;
		
		private boolean repeat;
		
		public AsyncReceiver(ObjectInputStream stream, InstancePacketReceiver receiver, boolean repeat) throws IOException
		{
			this.stream = stream;
			this.receiver = receiver;
			this.repeat = repeat;
		}
		
		@Override
		public void run() 
		{
			try 
			{
				do
				{
					InstancePacket packet = (InstancePacket)this.stream.readObject();
					this.receiver.receivePacket(packet, InstancePacketConnection.this);
				}
				while (this.repeat);
			} 
			catch (IOException e)
			{
				Log.e("Connection to " + connection.getInetAddress() + " interrupted");
			}
			catch (ClassNotFoundException e)
			{
				Log.e("Invalid update from " + connection.getInetAddress() + ", terminating connection");
			} 
			available = false;
		}
	}
	
	@Override
	public String toString()
	{
		return this.connection.getInetAddress().getHostAddress();
	}
}
