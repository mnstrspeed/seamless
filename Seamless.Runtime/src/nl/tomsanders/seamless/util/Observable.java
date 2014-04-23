package nl.tomsanders.seamless.util;

import java.util.ArrayList;
import java.util.Iterator;

public class Observable<T>
{
	private ArrayList<Observer<T>> observers =
			new ArrayList<Observer<T>>();
	private boolean changed = false;
	
	public void addObserver(Observer<T> observer)
	{
		this.observers.add(observer);
	}
	
	public void deleteObserver(Observer<T> observer)
	{
		this.observers.remove(observer);
	}
	
	public void setChanged()
	{
		this.changed = true;
	}
	
	public void clearChanged()
	{
		this.changed = false;
	}
	
	public boolean hasChanged()
	{
		return this.changed;
	}
	
	public void notifyObservers(T observable)
	{
		if (this.changed)
		{
			for (Iterator<Observer<T>> iterator = this.observers.iterator(); iterator.hasNext();)
			{
				Observer<T> observer = iterator.next();
				if (observer != null)
				{
					observer.notify(this, observable);
				}
				else
				{
					iterator.remove();
				}
			}
		}
	}
}
