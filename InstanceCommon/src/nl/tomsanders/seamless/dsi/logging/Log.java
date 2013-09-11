package nl.tomsanders.seamless.dsi.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log 
{
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	
	public static void v(Object message)
	{
		Log.addMessage(LogLevel.VERBOSE, message);
	}
	
	public static void w(Object message)
	{
		Log.addMessage(LogLevel.WARNING, message);
	}
	
	public static void e(Object message)
	{
		Log.addMessage(LogLevel.ERROR, message);
	}
	
	public static void addMessage(LogLevel level, Object message)
	{
		System.out.println(dateFormat.format(new Date()) + " - " + message.toString());
	}
}
