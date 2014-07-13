package nl.tomsanders.seamless.networking;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import nl.tomsanders.seamless.logging.Log;

public class ObjectConnection<T extends Serializable> implements Closeable
{
	public static interface ObjectReceiver<T extends Serializable>
	{
		public void receivePacket(T packet, ObjectConnection<T> connection) throws IOException;
	}
	
	private final Socket connection;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	private boolean isReceivingAsync = false;
	private Thread asyncThread;
	private boolean available;
	
	private List<ObjectReceiver<T>> receivers;
	
	public ObjectConnection(Socket socket) throws IOException
	{
		this.connection = socket;
		this.outputStream = new ObjectOutputStream(this.connection.getOutputStream());
		this.inputStream = new ObjectInputStream(this.connection.getInputStream()); 
		// Blocked until remote ObjectOutputStream is initialized
		
		this.receivers = new ArrayList<ObjectReceiver<T>>();
		this.available = true;
	}
	
	public void addReceiver(ObjectReceiver<T> receiver)
	{
		this.receivers.add(receiver);
	}
	
	public void deleteReceiver(ObjectReceiver<T> receiver)
	{
		this.receivers.remove(receiver);
		if (this.isReceivingAsync && this.receivers.isEmpty())
		{
			this.asyncThread.interrupt();
			try
			{
				this.close();
			}
			catch (IOException ex)
			{
				// "if an I/O error occurs when closing this socket."
				// Nothing we can do about this
			}
		}
	}
	
	public void send(T packet) throws IOException
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
	
	@SuppressWarnings("unchecked")
	public T receive() throws ClassNotFoundException, IOException
	{
		if (this.isReceivingAsync)
		{
			throw new IllegalStateException("Blocked receiving is not available "
					+ "while receiving asynchronously");
		}
		
		try
		{
			return (T)this.inputStream.readObject();
		}
		catch (IOException ex)
		{
			Log.e("Connection to " + connection.getInetAddress() + " interrupted");
			this.available = false;
			
			throw ex;
		}
	}
	
	public void receiveAsync(ObjectReceiver<T> receiver, boolean repeat) throws IOException
	{
		this.receivers.add(receiver);
		this.receiveAsync(repeat);
	}
	
	public void receiveAsync(boolean repeat) throws IOException
	{
		this.isReceivingAsync = true;
		
		this.asyncThread = new Thread(new AsyncReceiver(this.inputStream, this::receivePacket, repeat));
		this.asyncThread.start();
	}
	
	public void receivePacket(T packet, ObjectConnection<T> connection) throws IOException
	{
		for (ObjectReceiver<T> receiver : this.receivers)
		{
			receiver.receivePacket(packet, connection);
		}
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
		private ObjectReceiver<T> receiver;
		
		private boolean repeat;
		
		public AsyncReceiver(ObjectInputStream stream, ObjectReceiver<T> receiver, boolean repeat) throws IOException
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
					@SuppressWarnings("unchecked")
					T packet = (T)this.stream.readObject();
					
					this.receiver.receivePacket(packet, ObjectConnection.this);
				}
				while (this.repeat);
			} 
			catch (IOException e)
			{
				Log.v("Connection to " + connection.getInetAddress() + " interrupted");
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
