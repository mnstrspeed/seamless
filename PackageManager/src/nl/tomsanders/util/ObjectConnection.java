package nl.tomsanders.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import nl.tomsanders.seamless.dsi.logging.Log;

public class ObjectConnection<T extends Serializable>
{
	private final Socket connection;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	private boolean isReceivingAsync = false;
	private Thread asyncThread;
	
	public ObjectConnection(Socket socket) throws IOException
	{
		this.connection = socket;
		this.outputStream = new ObjectOutputStream(this.connection.getOutputStream());
		this.inputStream = new ObjectInputStream(this.connection.getInputStream()); 
		// Blocked until remote ObjectOutputStream is initialized
	}
	
	public Socket getSocket()
	{
		return this.connection;
	}
	
	public void close() throws IOException
	{
		this.connection.close();
	}
	
	public void send(T object) throws IOException
	{
		this.outputStream.writeObject(object);
		this.outputStream.flush();
	}
	
	@SuppressWarnings("unchecked")
	public T receive() throws ClassNotFoundException, IOException
	{
		if (this.isReceivingAsync)
		{
			throw new IllegalStateException("Blocked receiving is not available "
					+ "while receiving asynchronously");
		}
		
		return (T)this.inputStream.readObject();
	}
	
	public void receiveAsync(ObjectReceiver<T> receiver, boolean repeat) throws IOException
	{
		this.isReceivingAsync = true;
		
		this.asyncThread = new Thread(new AsyncReceiver(this.inputStream, receiver, repeat));
		this.asyncThread.start();
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
				Log.e("Connection to " + connection.getInetAddress() + " interrupted (" + e.getMessage() + ")");
			}
			catch (ClassNotFoundException e)
			{
				Log.e("Invalid update from " + connection.getInetAddress() + ", terminating connection");
			} 
		}
	}
}
