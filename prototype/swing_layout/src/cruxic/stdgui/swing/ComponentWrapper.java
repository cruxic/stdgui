package cruxic.stdgui.swing;

import cruxic.stdgui.layout.*;

import javax.swing.*;
import java.awt.*;

/**

 */
public class ComponentWrapper
	implements ComponentInterface
{
	private Component swing_component;

	public ComponentWrapper(Component swing_component)
	{
		this.swing_component = swing_component;
	}

	public int[] getPreferredSize()
	{
		Dimension dim = swing_component.getPreferredSize();
		return new int[] {dim.width, dim.height};
	}

	public Object getComponent()
	{
		return swing_component;
	}

	public void setPositionAndSize(int x, int y, int w, int h)
	{
		swing_component.setBounds(x, y, w, h);
	}

	public int getBaseline(int[] preferredSize)
	{
		int val = swing_component.getBaseline(preferredSize[0], preferredSize[1]);
		if (val > 0)
			return val;
		else  //a baseline of zero is nonsense, right?
			return 0;
	}


	public String debugString()
	{
		if (swing_component instanceof JButton)
			return ((JButton)swing_component).getText();
		else if (swing_component instanceof JLabel)
			return ((JLabel)swing_component).getText();
		else if (swing_component instanceof JTextField)
			return ((JTextField)swing_component).getText();
		else
			return swing_component.getClass().getSimpleName();
	}
}
