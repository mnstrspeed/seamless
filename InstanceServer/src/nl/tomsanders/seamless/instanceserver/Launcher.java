package nl.tomsanders.seamless.instanceserver;

import java.io.IOException;

import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.logging.LogLevel;

public class Launcher
{
	public static void main(String[] args)
	{
		Log.LEVEL = LogLevel.VERBOSE;
		try
		{	
			new InstanceServer().start();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
