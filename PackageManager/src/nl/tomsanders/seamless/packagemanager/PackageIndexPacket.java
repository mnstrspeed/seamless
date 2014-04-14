package nl.tomsanders.seamless.packagemanager;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class PackageIndexPacket extends PackageManagerPacket
{
	private PackageIndex packageIndex;
	
	public PackageIndexPacket(PackageIndex packageIndex) 
	{
		super(PacketManagerPacketType.INDEX);
		this.packageIndex = packageIndex;
	}
	
	public List<Package> getPackages()
	{
		return this.packageIndex.getPackages();
	}
}
