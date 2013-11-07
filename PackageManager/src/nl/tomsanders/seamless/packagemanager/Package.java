package nl.tomsanders.seamless.packagemanager;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Package implements Serializable
{
	private String name;
	private int version;
	private boolean isLatest;
	
	public Package(String name, int version)
	{
		this.name = name;
		this.version = version;
		this.isLatest = true;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public int getVersion()
	{
		return this.version;
	}
	
	public boolean isLatestVersion()
	{
		return this.isLatest;
	}
	
	public void setIsLatestVersion(boolean a)
	{
		this.isLatest = a;
	}
	
	@Override
	public boolean equals(Object b)
	{
		if (b instanceof Package)
		{
			Package packageB = (Package)b;
			return packageB.getName().equals(this.getName()) &&
					packageB.getVersion() == this.getVersion();
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public String toString()
	{
		return this.name + "-" + this.version;
	}
}
