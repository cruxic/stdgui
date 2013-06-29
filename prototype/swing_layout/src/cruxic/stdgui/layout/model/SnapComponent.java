package cruxic.stdgui.layout.model;

/**

 */
public class SnapComponent
{
	public enum Type
	{
		BUTTON,
		LABEL,
		ENTRY
	}

	public Type type;
	public String text;

	public SnapComponent(Type type)
	{
		this.type = type;
	}
}
