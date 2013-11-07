package nl.tomsanders.seamless.packagemanager;

@SuppressWarnings("serial")
public class PackageRequestPacket extends PackageManagerPacket
{
	private Package pack;
	
	public PackageRequestPacket(Package pack) 
	{
		super(PacketManagerPacketType.REQUEST);
		this.pack = pack;
	}
	
	public Package getPackage()
	{
		return this.pack;
	}
}
