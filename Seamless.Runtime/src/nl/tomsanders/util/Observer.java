package nl.tomsanders.util;

public interface Observer<T>
{
	public void notify(Observable<T> observable, T data);
}
