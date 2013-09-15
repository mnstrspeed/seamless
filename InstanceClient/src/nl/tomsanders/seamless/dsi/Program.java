package nl.tomsanders.seamless.dsi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class Program
{	
	public static void main(String[] args)
	{	
		Reference<Model> model = InstanceManager.getInstance(Model.class);
		model.get().addNote("Note added by " + getLocalHostName() + 
				" at " + new Date().toString());
		
		System.out.println("Model now contains: ");
		for (String note : model.get().getNotes())
		{
			System.out.println(note);
		}
	}
	
	private static String getLocalHostName() {
		try
		{
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex)
		{
			return "unknown host";
		}
	}
}
