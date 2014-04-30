package nl.tomsanders.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public abstract class Program 
{
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Argument 
	{
		String[] tags();
	}
	
	public Program withArguments(String[] args) 
	{
		List<String> arguments = Arrays.asList(args);
		
		try  
		{
			for (Field field : this.getClass().getFields()) 
			{
				if (field.isAnnotationPresent(Argument.class)) 
				{
					Argument argument = field.getAnnotation(Argument.class);
					boolean isFlag = field.getType().equals(boolean.class);
					
					int offset = matchInArguments(argument, arguments, isFlag ? 0 : 1);
					if (offset >= 0)
						field.set(this, isFlag ? true : arguments.get(offset + 1));
				}
			}
			
			boolean matched = false;
			for (Method method : this.getClass().getMethods()) 
			{
				if (method.isAnnotationPresent(Argument.class)) 
				{
					Argument argument = method.getAnnotation(Argument.class);
					int parameterCount = method.getParameterTypes().length;

					int offset = matchInArguments(argument, arguments, parameterCount);
					if (offset >= 0) 
					{
						method.invoke(this, arguments.subList(offset + 1, 
								offset + 1 + parameterCount).toArray());
						matched = true;
					}
					
					if (matched) 
						break;
				}
			}
			
			if (!matched) 
				this.printUsage();
			
		} 
		catch (Exception ex) 
		{
			this.printUsage();
		}
		
		return this;
	}
	
	private static int matchInArguments(Argument argument, List<String> arguments, int parameterCount) 
	{
		for (String tag : argument.tags()) 
		{
			for (int offset = 0; offset < arguments.size() - parameterCount; offset++) 
			{
				if (arguments.get(offset).equals(tag)) 
					return offset;
			}
		}
		return -1;
	}

	protected void printUsage() 
	{
		System.out.println("Usage:");
	}
}
