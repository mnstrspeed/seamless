package nl.tomsanders.seamless.dsi;

public class Program
{	
	public static void main(String[] args)
	{
		/*Reference<Model> model = OldInstanceManager.getInstance(Model.class);
		
		Model external = new Model();
		external.addNote("We do what we must, because we can.");
		external.addNote("For the good of all of us, except the ones who are dead.");
		external.addNote("It's hard to overstate my satisfaction.");
		
		OldInstanceManager.updateInstance(Model.class, external);
		
		for (String str : model.get().getNotes())
			System.out.println(str);
		*/
		
		Reference<Model> model = InstanceManager.getInstance(Model.class);
		System.out.println("Program.main: Model loaded");
		
		System.out.println("Program.main: Model contents:");
		for (String note : model.get().getNotes())
		{
			System.out.println(note);
		}
		
		System.out.println("Program.main: updating Model");
		model.get().addNote("For the good of all of us, except the ones who are dead.");
		System.out.println("Program.main: Model updated");
	}
}
