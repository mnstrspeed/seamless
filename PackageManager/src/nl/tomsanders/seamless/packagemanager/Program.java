package nl.tomsanders.seamless.packagemanager;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;

import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.logging.LogLevel;

public class Program extends nl.tomsanders.util.Program 
{
	private static final int PACKAGE_MANAGER_PORT = 1812;
	
	@Argument(tags = "verbose")
	public boolean verbose = false;
	
	@Argument(tags = "start")
	public void startPackageManager() 
	{
		if (verbose)
		{
			Log.LEVEL = LogLevel.VERBOSE;
		}
		
		try
		{
			new PackageManager().start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	@Argument(tags = "add")
	public void updatePackage(String packageName, String packageVersion, String packagePath) 
	{
		if (new File(packagePath).isFile())
		{
			try
			{
				Package pack = new Package(packageName, Integer.parseInt(packageVersion));
				PackagePacket packet = new PackagePacket(pack, Paths.get(packagePath));
				
				PackageManagerConnection connection = new PackageManagerConnection(
						new Socket("127.0.0.1", PACKAGE_MANAGER_PORT));
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
			System.out.println("Specified file does not exist");
		}
	}

	@Argument(tags = "clean")
	public void cleanPackageIndex() 
	{
		throw new RuntimeException("Clean not implemented");
	}
	
	@Override
	protected void printUsage()
	{
		System.out.println("Usage:");
		System.out.println("  update-manager start [-v]");
		System.out.println("  update-manager add <name> <version> <path>");
		System.out.println("  update-manager clean");
		
		System.exit(1);
	}
	
	public static void main(String[] args)
	{
		new Program().withArguments(args);
	}
}
