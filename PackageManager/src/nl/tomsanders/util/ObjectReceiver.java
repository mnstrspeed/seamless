package nl.tomsanders.util;

import java.io.Serializable;

public interface ObjectReceiver<T extends Serializable>
{
	public void receivePacket(T object, ObjectConnection<T> connection);
}
