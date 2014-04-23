package nl.tomsanders.seamless.packagemanager;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class PackageIndexPacket extends PackageManagerPacket
{
	private ArrayList<Package> packages;
	
	public PackageIndexPacket(PackageIndex packageIndex) 
	{
		super(PacketManagerPacketType.INDEX);
		this.packages = packageIndex.getPackages();
	}
	
	public List<Package> getPackages()
	{
		return this.packages;
	}
}
