package nl.tomsanders.seamless.packagemanager;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import nl.tomsanders.seamless.dsi.logging.Log;
import nl.tomsanders.seamless.dsi.logging.LogLevel;

public class Program 
{
	private static final int PACKAGE_MANAGER_PORT = 1812;
	
	public static void main(String[] args)
	{
		List<String> argumentList = Arrays.asList(args);
		boolean verbose = argumentList.contains("-v") || argumentList.contains("-verbose") ||
				argumentList.contains("--v") || argumentList.contains("--verbose");
		String action = args.length > (verbose ? 1 : 0) ? args[0] : "start";
		
		if (verbose)
		{
			Log.LEVEL = LogLevel.VERBOSE;
		}
		
		if (action.equals("start"))
		{
			startPackageManager();
		}
		else if (action.equals("add") && args.length > 3 + (verbose ? 1 : 0))
		{
			String packageName = args[1];
			String packagePath = args[3];
			int packageVersion = Integer.parseInt(args[2]);
			
			updatePackage(packageName, packagePath, packageVersion);
		}
		else if (action.equals("clean"))
		{
			cleanPackageIndex();
		}
	}
	
	private static void startPackageManager() 
	{
		try
		{
			new PackageManager().start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void updatePackage(String packageName, String packagePath, int packageVersion) 
	{
		if (new File(packagePath).isFile())
		{
			try
			{
				Package pack = new Package(packageName, packageVersion);
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

	private static void cleanPackageIndex() 
	{
		throw new RuntimeException("Clean not implemented");
	}
}
