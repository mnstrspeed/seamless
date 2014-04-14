package nl.tomsanders.seamless.packagemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import nl.tomsanders.seamless.dsi.logging.Log;

public class PackageIndex 
{
	private static final String PACKAGE_DIR = "packages";
	private static final String INDEX_FILE = "index.dat";
	
	private ArrayList<Package> packages;
	
	private PackageIndex() 
	{
	}
	
	@SuppressWarnings("unchecked")
	public static PackageIndex load() throws FileNotFoundException, IOException 
	{
		PackageIndex index = new PackageIndex();
		
		File indexFile = new File(PACKAGE_DIR + "/" + INDEX_FILE);
		if (indexFile.isFile())
		{
			Log.v("Loading package index");
			try
			{
				ObjectInputStream deserializer = new ObjectInputStream(
						new FileInputStream(indexFile));
				index.packages = (ArrayList<Package>)deserializer.readObject();
				deserializer.close();
			}
			catch (ClassNotFoundException ex)
			{
				throw new RuntimeException(ex);
			}
		}
		else
		{
			Log.v("No package index detected; creating new package index");
			
			index.packages = new ArrayList<Package>();
			index.save();
		}
		
		return index;
	}
	
	public ArrayList<Package> getPackages() 
	{
		return this.packages;
	}
	
	public boolean containsPackage(Package pack) 
	{
		return this.packages.contains(pack);
	}
	
	public void save() throws FileNotFoundException, IOException
	{
		Log.v("Saving package index to disk");
		
		ObjectOutputStream serializer = new ObjectOutputStream(
				new FileOutputStream(PACKAGE_DIR + "/" + INDEX_FILE));
		serializer.writeObject(this.packages);
		serializer.close();
	}
	
	public void add(Package pack) 
	{
		this.packages.add(pack);
	}
	
	public static String getPackageDir()
	{
		return PACKAGE_DIR;
	}
	
	public static String getIndexFile()
	{
		return INDEX_FILE;
	}
}
