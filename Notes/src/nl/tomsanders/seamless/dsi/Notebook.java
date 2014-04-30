package nl.tomsanders.seamless.dsi;

import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;

import nl.tomsanders.seamless.util.Observable;

public class Notebook extends Observable<Notebook> implements Serializable
{
	private static final long serialVersionUID = -2843554972772300125L;
	
	private ArrayList<Note> notes;
	
	public Notebook()
	{
		this.notes = new ArrayList<Note>();
	}
	
	public void addNote(Note note)
	{
		this.notes.add(note);
		
		this.setChanged();
		this.notifyObservers(this);
	}
	
	public void removeNote(int noteIndex)
	{
		this.removeNote(this.notes.get(noteIndex));
	}
	
	public void removeNote(Note note)
	{
		this.notes.remove(note);
		
		this.setChanged();
		this.notifyObservers(this);
	}
	
	public List<Note> getNotes()
	{
		return new ArrayList<Note>(this.notes);
	}
}
