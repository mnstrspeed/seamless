import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;

import nl.tomsanders.seamless.packagemanager.Package;
import nl.tomsanders.seamless.packagemanager.PackageManagerConnection;
import nl.tomsanders.seamless.packagemanager.PackagePacket;


public class PackageUpdater 
{
	public static void main(String[] args)
	{
		if (args.length >= 1)
		{
			String action = args[0];
			if (action.equals("update") && args.length == 4)
			{
				String packageName = args[1];
				int version = Integer.parseInt(args[2]);
				File file = new File(args[3]);
				
				if (file.isFile())
				{
					try
					{
						Package pack = new Package(packageName, version);
						
						Socket socket = new Socket("127.0.0.1", 1812);
						PackageManagerConnection connection = new PackageManagerConnection(socket);
						
						PackagePacket packet = new PackagePacket(pack, Paths.get(args[3]));
						connection.send(packet);
						connection.close();
					}
					catch (IOException ex)
					{
						System.out.println("Failed to update package: " + ex.getMessage());
					}
				}
				else
				{
					System.out.println("Specified file not found");
				}
			}
			else
			{
				printUsage();
			}
		}
		else
		{
			printUsage();
		}
	}

	private static void printUsage() 
	{
		System.out.println("update <name> <version> <path>");
	}
}
