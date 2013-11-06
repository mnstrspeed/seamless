package nl.tomsanders.seamless.dsi;

import java.io.Serializable;
import java.util.Date;

public class Note implements Serializable
{
	private static final long serialVersionUID = -5015423143170033889L;
	
	private String text;
	private Date time;
	
	public Note(String text)
	{
		this(text, new Date());
	}
	
	public Note(String text, Date time)
	{
		this.text = text;
		this.time = time;
	}
	
	public String getText()
	{
		return this.text;
	}
	
	public Date getTime()
	{
		return this.time;
	}
	
	@Override
	public String toString()
	{
		return this.text + " [" + this.time.toString() + "]";
	}
}
