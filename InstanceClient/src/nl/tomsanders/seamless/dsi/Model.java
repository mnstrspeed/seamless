package nl.tomsanders.seamless.dsi;

import java.io.Serializable;
import java.util.ArrayList;

import nl.tomsanders.util.Observable;

@SuppressWarnings("serial")
public class Model extends Observable<Model> implements Serializable
{
	private ArrayList<String> notes;
	
	public Model()
	{
		this.notes = new ArrayList<String>();
	}
	
	public void addNote(String note)
	{
		this.notes.add(note);
		
		this.setChanged();
		this.notifyObservers(this);
	}
	
	public void removeNote(String note)
	{
		this.notes.remove(note);
		
		this.setChanged();
		this.notifyObservers(this);
	}
	
	public ArrayList<String> getNotes()
	{
		return new ArrayList<String>(this.notes);
	}
}
