package nl.tomsanders.seamless.dsi;

public class Program
{	
	public static void main(String[] args)
	{	
		Reference<Notebook> notebook = InstanceManager.getInstance(Notebook.class);

		if (args.length == 2)
		{
			if (args[0].equals("add"))
			{
				notebook.get().addNote(new Note(args[1]));
			}
			else if (args[0].equals("remove"))
			{
				notebook.get().removeNote(Integer.parseInt(args[1]));
			}
			else
			{
				System.out.println("Usage: add <note> | remove <note index>");
			}
		}
		else if (args.length != 0)
		{
			System.out.println("Usage: add <note> | remove <note index>");
		}

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
		
		System.exit(0);
	}
}
