package cruxic.stdgui.swing.tests;

import cruxic.stdgui.swing.*;
import cruxic.stdgui.layout.model.*;
import cruxic.stdgui.layout.model.Box;  //explicit import to disambiguate

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.util.IdentityHashMap;
import java.awt.Component;

/**

 */
public class DisplayXML extends JFrame
{
	public DisplayXML()
	{
		setSize(640, 480);
		setTitle(getClass().getSimpleName());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.err.println("Missing file argument");
			System.exit(2);
		}

		try
		{
			DisplayXML frm = new DisplayXML();

			frm.layoutFromXML(new File(args[0]));

			frm.setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void layoutFromXML(File file)
		throws IOException
	{
		ModelFromXML parser = new ModelFromXML();
		Box rootBox = parser.parseLayoutFromXMLFile(file);
		rootBox.debugPrint("");

		//temporary layout manager
		setLayout(new FlowLayout());

		//Construct java Components for each model component
		IdentityHashMap<SnapComponent, ComponentWrapper> componentMap = new IdentityHashMap<SnapComponent, ComponentWrapper>(64);
		for (SnapComponent model_component: rootBox.getComponents())
		{
			java.awt.Component java_component;

			switch (model_component.type)
			{
				case BUTTON:
					java_component = new JButton(model_component.text);
					break;
				case LABEL:
					JLabel lbl = new JLabel(model_component.text);
					lbl.setFont(new Font("Sans", Font.PLAIN, 32));
					java_component = lbl;
					break;
				case ENTRY:
					JTextField txt = new JTextField(model_component.text);
					txt.setFont(new Font("Sans", Font.PLAIN, 8));
					java_component = txt;
					break;
				default:
					throw new UnsupportedOperationException(model_component.type.name());
			}

			componentMap.put(model_component, new ComponentWrapper(java_component));
		}

		//Add all java components to the form
		for (ComponentWrapper java_component: componentMap.values())
		{
			add((Component)java_component.getComponent());
		}

		SnapLayout layout = new SnapLayout(rootBox, componentMap);

		layout.root.computePreferredSizes();

		//If the preferred size of the layout nearing the default window size, increase the window size
		int windowW = Math.max(getWidth(), layout.root.preferredSize[0] + 100);
		int windowH = Math.max(getHeight(), layout.root.preferredSize[1] + 100);
		setSize(windowW, windowH);

		setLayout(layout);
	}


}
