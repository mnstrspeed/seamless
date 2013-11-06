package nl.tomsanders.seamless.dsi;

import nl.tomsanders.seamless.dsi.logging.Log;
import nl.tomsanders.util.Observable;
import nl.tomsanders.util.Observer;

public class InstanceMonitor<T extends Observable<T>> implements Observer<T>
{
	private final Reference<T> instanceReference;
	private T currentInstance;
	
	private final Observer<Reference<T>> referenceObserver =
			new Observer<Reference<T>>()
			{
				@Override
				public void notify(Observable<Reference<T>> observable,
						Reference<T> data)
				{
					referenceUpdated();
				}
			};
	
	public InstanceMonitor(Reference<T> instanceReference)
	{
		this.instanceReference = instanceReference;
		this.instanceReference.addObserver(referenceObserver);
		this.referenceUpdated();
	}
	
	private void referenceUpdated()
	{
		Log.v(this.getClass().getName() + "@" + Integer.toHexString(this.hashCode()) + " was re-referenced");
		
		// Unregister from old observable, if exists
		if (this.currentInstance != null)
		{
			this.currentInstance.deleteObserver(this);
		}
		
		// Register to new observable
		this.currentInstance = this.instanceReference.get();
		this.currentInstance.addObserver(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void notify(Observable<T> observable, T data)
	{
		Log.v(data.getClass().getName() + "@" + Integer.toHexString(data.hashCode()) + " was modified");
		InstanceManager.updateInstance(data.getClass(), data);
	}
}
