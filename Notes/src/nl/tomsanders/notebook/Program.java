package nl.tomsanders.notebook;

import nl.tomsanders.seamless.runtime.Reference;
import nl.tomsanders.seamless.runtime.Runtime;

public class Program extends nl.tomsanders.util.Program
{	
	private Reference<Notebook> notebook;
	
	public Program()
	{
		this.notebook = Runtime.getInstance(Notebook.class);
	}
	
	@Argument(tags = "list")
	public void list()
	{
		this.printNotebook();
	}
	
	@Argument(tags = "add")
	public void addNote(String note)
	{
		notebook.get().addNote(new Note(note));
		this.printNotebook();
	}
	
	@Argument(tags = "remove")
	public void removeNote(String index)
	{
		notebook.get().removeNote(Integer.parseInt(index));
		this.printNotebook();
	}
	
	private void printNotebook()
	{
		System.out.println();
		if (notebook.get().getNotes().size() != 0)
		{
			// Print notes
			for (Note note : notebook.get().getNotes())
			{
				System.out.println("o   " +  note);
			}
		}
		else
		{
			System.out.println("Notebook is empty");
		}
		System.out.println();
	}
	
	@Override
	public void printUsage()
	{
		System.out.println("Usage: list | add <note> | remove <note index>");
	}
	
	public static void main(String[] args)
	{	
		new Program().withArguments(args);
		Runtime.exit();
	}
}
