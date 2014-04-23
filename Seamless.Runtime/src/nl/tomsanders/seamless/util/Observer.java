package nl.tomsanders.seamless.util;

public interface Observer<T>
{
	public void notify(Observable<T> observable, T data);
}
