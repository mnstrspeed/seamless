package nl.tomsanders.seamless.dsi.networking;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import nl.tomsanders.seamless.dsi.logging.Log;

public class InstancePacketConnection implements Closeable
{
	private final Socket connection;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	private boolean isReceivingAsync = false;
	private Thread asyncThread;
	
	public InstancePacketConnection(Socket socket) throws IOException
	{
		this.connection = socket;
		this.outputStream = new ObjectOutputStream(this.connection.getOutputStream());
		this.inputStream = new ObjectInputStream(this.connection.getInputStream()); 
		// Blocked until remote ObjectOutputStream is initialized
	}
	
	public void send(InstancePacket packet) throws IOException
	{
		this.outputStream.writeObject(packet);
		this.outputStream.flush();
	}
	
	public InstancePacket receive() throws ClassNotFoundException, IOException
	{
		if (this.isReceivingAsync)
		{
			throw new IllegalStateException("Blocked receiving is not available "
					+ "while receiving asynchronously");
		}
		
		return (InstancePacket)this.inputStream.readObject();
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
	}
	
	public Socket getSocket()
	{
		return this.connection;
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
			catch (IOException | ClassNotFoundException e) 
			{
				Log.e("Stopped listening for packets from " + connection.getInetAddress());
			} 
		}
	}
}
