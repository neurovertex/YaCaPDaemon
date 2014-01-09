package eu.neurovertex.yacapd;

import eu.neurovertex.yacapd.gui.Gui;

import java.awt.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author NeuroVertex
 *         Date: 12/09/13, 16:29
 */
public class Main {

	public static void main(String args[]) throws IOException {
		// TODO add command-line arguments
		Preferences prefs = Preferences.userNodeForPackage(YacapPageProcessor.class);
		Logger.getLogger(Main.class.getPackage().getName()).setLevel(Level.INFO); // Sets logging level

		Logger root = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		for (Handler h : root.getHandlers())
			root.removeHandler(h);

		root.addHandler(new StreamHandler(System.out, new SimpleFormatter()));

		if (prefs.get("username", "").isEmpty() || prefs.get("password", "").isEmpty())
			promptLogin(prefs);

		YacapPageProcessor processor = new YacapPageProcessor();
		if (!GraphicsEnvironment.isHeadless()) {
			new Gui(processor);
		} else
			System.out.println("Skipping GUI (Headless environment)");
		new Thread(processor).start();
	}

	public static void promptLogin(Preferences prefs) {
		Scanner sc = new Scanner(System.in);
		String line;
		System.out.println("Enter your CaS username.");
		do {
			line = sc.nextLine();
		} while (line == null || line.length() == 0);
		prefs.put("username", line);
		System.out.println("Enter your CaS password.");
		do {
			line = sc.nextLine();
		} while (line == null || line.length() == 0);
		prefs.put("password", line);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
}
