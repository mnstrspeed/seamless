package nl.tomsanders.seamless.packagemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("serial")
public class PackagePacket extends PackageManagerPacket
{
	private Package pack;
	private byte[] data;
	
	public PackagePacket(Package pack, Path path) throws IOException
	{
		this(pack, Files.readAllBytes(path));
	}
	
	public PackagePacket(Package pack, byte[] data) 
	{
		super(PacketManagerPacketType.PACKAGE);
		this.pack = pack;
		this.data = data;
	}
	
	public byte[] getData()
	{
		return this.data;
	}
	
	public Package getPackage()
	{
		return this.pack;
	}
}
