package nl.tomsanders.seamless.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public class InstanceData implements Serializable
{
	private final byte[] data;
	
	public InstanceData(Serializable serializable) throws IOException
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(outputStream);
		stream.writeObject(serializable);
		
		this.data = outputStream.toByteArray();
		stream.close();
	}
	
	public InstanceData(byte[] data)
	{
		this.data = data;
	}
	
	public Object getObject() throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream inputStream = new ByteArrayInputStream(this.data);
		ObjectInputStream stream = new ObjectInputStream(inputStream);
		
		return stream.readObject();
	}
	
	public byte[] getData()
	{
		return this.data;
	}
}
