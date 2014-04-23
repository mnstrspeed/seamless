package nl.tomsanders.seamless.runtime;

import nl.tomsanders.seamless.util.Observable;

public class Reference<T> extends Observable<Reference<T>>
{
	private T model;
	
	public Reference(T model)
	{
		this.model = model;
	}
	
	public T get()
	{
		return this.model;
	}
	
	public void setModel(T model)
	{
		this.model = model;
		
		this.setChanged();
		this.notifyObservers(this);
	}
}
