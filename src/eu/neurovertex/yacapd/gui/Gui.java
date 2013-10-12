package eu.neurovertex.yacapd.gui;

import eu.neurovertex.yacapd.HTTPSDownloader;
import eu.neurovertex.yacapd.YacapPageProcessor;
import eu.neurovertex.yacapd.YacapPageProcessor.YacapState;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author NeuroVertex
 *         Date: 07/10/13, 13:02
 */
public class Gui implements ActionListener, Observer {
	private static final Map<YacapState, Image> stateIcons = new HashMap<>();
	private static final int iconWidth, iconHeight;
	private static final Logger log = Logger.getLogger(Gui.class.getName());

	static {
		Color transparency = new Color(0, 0, 0, 0);
		Dimension trayIconSize = SystemTray.getSystemTray().getTrayIconSize();
		iconWidth = trayIconSize.width;
		iconHeight = trayIconSize.height;

		Map<YacapState, BufferedImage> map = new HashMap<>();
		for (YacapState state : YacapState.values())
			map.put(state, new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB));

		Graphics2D g;
		g = map.get(YacapState.UNINITIALIZED).createGraphics();
		g.setColor(Color.GRAY);
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);

		g = map.get(YacapState.LOGIN).createGraphics();
		g.setColor(Color.BLUE.brighter());
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);

		g = map.get(YacapState.AUTH).createGraphics();
		g.setColor(Color.CYAN);
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);

		g = map.get(YacapState.BADLOGIN).createGraphics();
		g.setColor(Color.RED);
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);

		g = map.get(YacapState.CONNECTED).createGraphics();
		g.setColor(Color.GREEN);
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);

		g = map.get(YacapState.WAIT).createGraphics();
		g.setColor(Color.YELLOW);
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);

		g = map.get(YacapState.LOGOUT).createGraphics();
		g.setColor(Color.BLACK);
		g.setBackground(transparency);
		g.clearRect(0, 0, iconWidth - 1, iconHeight - 1);
		g.fillOval(0, 0, iconWidth - 1, iconHeight - 1);


		stateIcons.putAll(map);
	}

	private YacapPageProcessor processor;
	private HTTPSDownloader downloader;
	private YacapState lastState;
	private HTTPSDownloader.DownloaderStatus lastNetworkState;
	private TrayIcon icon;
	private MenuItem settingsMI, reconnectMI, logoutMI, exitMI;

	public Gui(YacapPageProcessor processor) {
		this.processor = processor;
		this.downloader = processor.getDownloader();
		icon = new TrayIcon(stateIcons.get(YacapState.UNINITIALIZED));
		icon.setToolTip("YaCaP daemon");

		PopupMenu menu = new PopupMenu();
		menu.add(settingsMI = new MenuItem("Settings")).setEnabled(false);
		menu.add(reconnectMI = new MenuItem("Reconnect"));
		menu.add(logoutMI = new MenuItem("Logout"));
		menu.add(exitMI = new MenuItem("Exit"));

		menu.addActionListener(this);
		icon.setPopupMenu(menu);

		SystemTray systray = SystemTray.getSystemTray();
		try {
			systray.add(icon);
		} catch (AWTException e) {
			log.log(Level.SEVERE, "Can't add icon to system tray.", e); // TODO add --nogui and/or --notray
			System.exit(-1);
		}
		processor.addObserver(this);
		downloader.addObserver(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Action received : " + e.getActionCommand());
		if (e.getActionCommand().equals(settingsMI.getActionCommand())) {
			System.err.println("Settings not implemented yet."); // TODO implement settings GUI
		} else if (e.getActionCommand().equals(reconnectMI.getActionCommand())) {
			processor.reconnect();
		} else if (e.getActionCommand().equals(logoutMI.getActionCommand())) {
			processor.logout();
		} else if (e.getActionCommand().equals(exitMI.getActionCommand())) {
			processor.stop();
		} else {
			throw new IllegalArgumentException("Unrecognized Action Command : " + e.getActionCommand());
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o == processor && arg instanceof YacapState) {
			if (arg == YacapState.EXIT) {
				SystemTray.getSystemTray().remove(icon);
			}
			lastState = (YacapState) arg;
			icon.setImage(stateIcons.get(lastState));

			boolean reco = true, logout = true;
			switch (lastState) {
				case UNINITIALIZED:
					logout = reco = false;
					break;
				case WAIT:
					logout = false;
					break;
				case LOGOUT:
				case LOGIN:
				case AUTH:
					reco = false;
					break;
			}
			reconnectMI.setEnabled(reco);
			logoutMI.setEnabled(logout);
		} else if (o == downloader && arg instanceof HTTPSDownloader.DownloaderStatus) {
			System.out.println(arg);
		}
	}
}
