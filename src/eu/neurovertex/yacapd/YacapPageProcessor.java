package eu.neurovertex.yacapd;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;


/**
 * This class is the main thread of the application. It is in charge of keeping up with the current state of the session, detect change of state (disconnections) and act appropriately.
 * It is also an Observable object that will notify its observers on state change.
 *
 * @author NeuroVertex
 *         Date: 12/09/13, 18:24
 */
public class YacapPageProcessor extends Observable implements Runnable, PreferenceChangeListener {
	private YacapState currentState = YacapState.UNINITIALIZED, nextState = null;
	private HTTPSDownloader downloader;
	private boolean exit = false, reconnect = true;
	private static final Preferences prefs = Preferences.systemNodeForPackage(YacapPageProcessor.class);
	private boolean loginCorrect = false;
	private static final Logger log = Logger.getLogger(YacapPageProcessor.class.getName());

	private static final String YacapSessionCookie = "YaCaP_session_ID", b64url = "aHR0cDovL2dvb2dsZS5jb20=";

	private static final HTTPRequest loginURL = new HTTPRequest("https://auth.univ-lorraine.fr/login?service=https://portail-crous.crous-nancy-metz.fr/authen-cas%3Furl%3DaHR0cDovL2dvb2dsZS5jb20%3D%26authen%3DAA%3A%3AAuthen-CAS-UL%3A%3AAuthz-LDAP-UL", new String[][]{{"Referer", "https://portail-crous.crous-nancy-metz.fr/?url=aHR0cDovL2dvb2dsZS5jb20="}}, new String[][]{}),
			authURL = new HTTPRequest("https://auth.univ-lorraine.fr/login?service=https://portail-crous.crous-nancy-metz.fr/authen-cas%3Furl%3D%26authen%3DAA%3A%3AAuthen-CAS-UL%3A%3AAuthz-LDAP-UL", new String[][]{{"Origin", "https://portail-crous.crous-nancy-metz.fr"}, {"Referer", "https://portail-crous.crous-nancy-metz.fr/?url=" + b64url}}, new String[][]{{"submit", "LOGIN"}}),
			logoutURL = new HTTPRequest("https://auth.univ-lorraine.fr/logout?service=https://portail-crous.crous-nancy-metz.fr/logout&gateway=1", new String[][]{}, new String[][]{{"submit", "logout"}}),
			popupURL = new HTTPRequest("https://portail-crous.crous-nancy-metz.fr/popup");


	public YacapPageProcessor() {
		downloader = new HTTPSDownloader(this);
		prefs.addPreferenceChangeListener(this);
		if (!prefs.get("username", "").isEmpty() && !prefs.get("password", "").isEmpty()) {
			loginCorrect = true;
			authURL.data.put("username", prefs.get("username", ""));
			authURL.data.put("password", prefs.get("password", ""));
		}
	}

	@Override
	public void run() {
		log.info("Processor started.");
		int retries = 0;
		while (!exit) {
			log.info("Next iteration (CS = " + currentState.name() + ")");
			int code;
			int delay = prefs.getInt("delay", 5000), errordelay = prefs.getInt("errordelay", 1000);
			switch (currentState) {
				case UNINITIALIZED:
					// Attempt to load a previously saved cookie
					if (GlobalCookieHandler.getDefault().loadSession())
						setCurrentState(YacapState.CONNECTED);
					else
						setCurrentState(YacapState.LOGIN);
					break;
				case LOGIN:
					code = downloader.getUrl(loginURL, "login");
					if (currentState == YacapState.LOGIN)
						try {
							Thread.sleep(errordelay + (1000 * retries++));
						} catch (InterruptedException ignore) {
							stop();
						}
					else
						retries = 0;
					if (code != 200)
						clearCookies();
					break;
				case AUTH:
					if (authURL.data.containsKey("lt") && authURL.data.containsKey("execution") && authURL.data.containsKey("_eventId")) {
						code = downloader.postUrl(authURL, "auth");
						if (code != 200)
							setCurrentState(YacapState.LOGIN);
						URI uri = URI.create(popupURL.getUrl());
						if (currentState == YacapState.CONNECTED) {
							GlobalCookieHandler.getDefault().setSessionCookie(uri.getHost(), YacapSessionCookie);
							GlobalCookieHandler.getDefault().save();
							retries = 0;
						} else if (currentState == YacapState.AUTH) {
							try {
								Thread.sleep(errordelay + 1000 * retries++);
							} catch (InterruptedException e) {
								stop();
							}
						}
					} else
						setCurrentState(YacapState.LOGIN);
					break;
				case BADLOGIN:
					System.out.println("Erroneous login information. Please re-enter your login and password.");
					while (!loginCorrect)
						synchronized (this) {
							try {
								wait();
							} catch (InterruptedException e) {
								stop();
							}
						}
					break;
				case CONNECTED:
					code = downloader.getUrl(popupURL, "popup");
					if (currentState == YacapState.CONNECTED && code == 200) {
						log.info("Popup refreshed.");
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							stop();
						}
					}
					if (code != 200)
						setCurrentState(YacapState.LOGIN);
					break;
				case LOGOUT:
					downloader.postUrl(logoutURL, "logout");
					clearCookies();
					try {
						Thread.sleep(errordelay);
					} catch (InterruptedException e) {
						stop();
					}
					if (reconnect)
						setCurrentState(YacapState.LOGIN);
					else
						setCurrentState(YacapState.WAIT);
					break;
				case WAIT:
					synchronized (this) {
						while (!reconnect && !exit)
							try {
								wait();
							} catch (InterruptedException e) {
								stop();
							}
						setCurrentState(YacapState.LOGIN);
					}
					break;
			}
			if (nextState != null) {
				setCurrentState(nextState);
				nextState = null;
			}
		}
		log.info("Exiting YacapProcessor thread");
		setCurrentState(YacapState.EXIT);
		System.exit(0);
	}

	private void clearCookies() {
		GlobalCookieHandler.getDefault().clearCookies();
	}


	/**
	 * When a bad login is detected, this variable is set to false. The thread will then wait until the login information is changed to continue.
	 *
	 * @return false if the thread is currently waiting
	 */
	public boolean isLoginCorrect() {
		return loginCorrect;
	}

	/**
	 * Logs out then logs back in
	 *
	 * @see eu.neurovertex.yacapd.YacapPageProcessor#logout()
	 */
	public void reconnect() {
		reconnect = true;
		loginCorrect = true;
		if (currentState == YacapState.WAIT || currentState == YacapState.BADLOGIN)
			synchronized (this) {
				notifyAll();
			}
		else
			this.nextState = YacapState.LOGOUT;
	}

	/**
	 * Logs out then wait indefinitely for user input.
	 *
	 * @see YacapPageProcessor#reconnect()
	 */
	public void logout() {
		this.reconnect = true;
		this.nextState = YacapState.LOGOUT;
	}

	/**
	 * This method analyses the document downloaded by the HTTPSDownloader and change the current execution state accordingly.
	 *
	 * @param doc the {@link Document} returned by the last HTTP request
	 */
	public void processDocument(Document doc) {
		Elements els;
		Element el;
		switch (currentState) {
			case LOGIN:
				Element form;
				els = doc.getElementsByTag("form");
				if (els.size() == 1)
					form = els.get(0);
				else
					form = doc.getElementById("fm1");
				els = form.getElementsByTag("input");
				int cnt = 0;
				for (Element e : els) {
					if (e.attr("name").equalsIgnoreCase("lt") || e.attr("name").equalsIgnoreCase("execution") || e.attr("name").equalsIgnoreCase("_eventId")) {
						System.out.println("authURL.put(\"" + e.attr("name") + "\", \"" + e.attr("value") + "\");");
						authURL.data.put(e.attr("name"), e.attr("value"));
						cnt++;
					}
				}
				if (cnt == 3)
					setCurrentState(YacapState.AUTH);
				break;
			case AUTH:
				if ((el = doc.getElementById("status")) != null && el.className().equalsIgnoreCase("errors")) {
					if (el.text().contains("LT-")) {
						clearCookies();
						setCurrentState(YacapState.LOGIN);
					} else {
						loginCorrect = false;
						setCurrentState(YacapState.BADLOGIN);
						System.out.println("Status got : " + el.text());
					}
				} else if (doc.title().toLowerCase().contains("yacap")) {
					System.err.println("Authentication fucking successful");
					setCurrentState(YacapState.CONNECTED);
				} else {
					Elements bs = doc.getElementsByTag("b");
					for (Element b : bs) {
						if (b.text().toLowerCase().contains(prefs.get("username", null).toLowerCase())) { // If CaS displays your username it probably about your account already being logged in somewhere else
							setCurrentState(YacapState.LOGOUT);
						}
					}
				}
				break;
			case CONNECTED:
				if (!"#86A3D4".equalsIgnoreCase(doc.body().attr("bgcolor"))) { // Background colour of the popup while connected
					setCurrentState(YacapState.LOGIN);
				}
				break;
			case LOGOUT:
				if (reconnect)
					setCurrentState(YacapState.AUTH);
				else
					stop();
				break;
		}
	}

	/**
	 * Sets the exit flag so that the thread exits its loop. Note that it won't exit immediately, especially if the HTTPSDownloader is currently waiting on a request.
	 */
	public synchronized void stop() {
		exit = true;
		downloader.stop(); // Notify the downloader that it should stop whenever the current request returns regardless of the result.
		notifyAll(); // in case of WAIT
	}

	private void setCurrentState(YacapState state) {
		if (state != currentState)
			setChanged();
		this.currentState = state;
		notifyObservers(state);
	}

	/**
	 * @return the current state of the session or application.
	 */
	public YacapState getCurrentState() {
		return currentState;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals("username") || evt.getKey().equals("password")) {
			synchronized (this) {
				authURL.data.put("username", prefs.get("username", ""));
				authURL.data.put("password", prefs.get("password", ""));
				loginCorrect = true;
				notifyAll();
			}
		}
	}

	public HTTPSDownloader getDownloader() {
		return downloader;
	}

	public enum YacapState {
		UNINITIALIZED,
		/**
		 * State where the application will load the login page to get the hidden fields of the form
		 */
		LOGIN,
		/**
		 * State following {@link YacapState#LOGIN} where the application will send the authentication information to the server.
		 */
		AUTH,
		/**
		 * State following {@link YacapState#AUTH} if the server reply with an erroneous login status
		 */
		BADLOGIN,
		/**
		 * State following {@link YacapState#AUTH} if the server didn't send any error.
		 */
		CONNECTED,
		/**
		 * State where the application will request from the server the temination of the session.
		 */
		LOGOUT,
		/**
		 * State set after logging out with the reconnect flag set to false. This is generally the result of {@link eu.neurovertex.yacapd.YacapPageProcessor#logout()}
		 */
		WAIT,
		/**
		 * This is a special state that is only set when the thread is about to exit, so that Observers get notified
		 */
		EXIT
	}

	/**
	 * Reperesents a HTTP request, including the target URL, the parameters (request headers) to set, and the POST data to add.
	 */
	protected static class HTTPRequest {
		private String url;
		private Map<String, String> parameters = new HashMap<>(), data = new HashMap<>();

		private HTTPRequest(String url) {
			this(url, new String[0][0], new String[0][0]);
		}

		private HTTPRequest(String url, String[][] parameters, String[][] data) {
			this.url = url;
			for (String[] str : parameters) {
				this.parameters.put(str[0], str[1]);
			}
			for (String[] str : data) {
				this.data.put(str[0], str[1]);
			}
		}

		public String getUrl() {
			return url;
		}

		public Map<String, String> getParameters() {
			return Collections.unmodifiableMap(parameters);
		}

		public Map<String, String> getData() {
			return Collections.unmodifiableMap(data);
		}
	}


}
