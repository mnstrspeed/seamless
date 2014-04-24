package nl.tomsanders.seamless.runtime;

import java.io.Serializable;

import nl.tomsanders.seamless.logging.Log;
import nl.tomsanders.seamless.util.Observable;
import nl.tomsanders.seamless.util.Observer;

/**
 * Observes the instance contained in a Reference. Whenever the reference
 * is re-referenced, the InstanceObserver automatically registers itself
 * to the new instance.
 */
public class InstanceObserver<T extends Observable<T> & Serializable> implements Observer<T>
{
	private final Reference<T> instanceReference;
	private final Class<T> type;
	private T currentInstance;
	
	public InstanceObserver(Reference<T> instanceReference, Class<T> type)
	{
		this.instanceReference = instanceReference;
		this.type = type;
		
		this.instanceReference.addObserver((o, d) -> referenceUpdated());
		this.referenceUpdated();
	}
	
	private void referenceUpdated()
	{
		Log.v(this.getClass().getName() + "@" + Integer.toHexString(this.hashCode()) + 
				" was re-referenced");
		
		// Stop observing old instance, if exists
		if (this.currentInstance != null)
		{
			this.currentInstance.deleteObserver(this);
		}
		
		// Start observing new instance
		this.currentInstance = this.instanceReference.get();
		this.currentInstance.addObserver(this);
	}

	@Override
	public void notify(Observable<T> observable, T data)
	{
		Runtime.updateInstance(this.type, data);
	}
}
