package eu.neurovertex.yacapd;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * This class is in charge of storing/retreiving cookies for HTTP requests and responses. In addition, it also stores the YaCaP session cookie in its Preferences to try and use the same session on next run (in case the application is restarted, this will avoid having to reauthenticate)
 *
 * @author NeuroVertex
 *         Date: 12/09/13, 17:41
 */
public class GlobalCookieHandler extends CookieHandler {
	private final Map<String, Set<String>> cookies = new HashMap<>();
	private static final Logger log = Logger.getLogger(GlobalCookieHandler.class.getName());
	private static final Preferences prefs = Preferences.userNodeForPackage(GlobalCookieHandler.class);
	private String sessionHost, sessionCookie;

	public static GlobalCookieHandler getDefault() {
		return (GlobalCookieHandler) CookieHandler.getDefault();
	}

	public GlobalCookieHandler() {
	}

	public boolean loadSession() {
		sessionCookie = prefs.get("sessionCookie", null);
		sessionHost = prefs.get("sessionHost", null);
		if (sessionHost != null) {
			String session = prefs.get("session", null);
			if (session != null) {
				Set<String> store = new HashSet<>();
				store.add(session);
				cookies.put(sessionHost, store);
				log.fine("Loaded session cookie \"" + session + "\" for \"" + sessionHost + "\".");
				return true;
			}
		} else {
			log.fine("No session cookie loaded");
		}
		return false;
	}

	public void clearCookies() {
		cookies.clear();
	}

	@Override
	public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
		Map<String, List<String>> ret = new HashMap<>();
		synchronized (cookies) {
			Set<String> store = cookies.get(uri.getHost());
			if (store != null) {
				ret.put("Cookie", Collections.unmodifiableList(new ArrayList<>(store)));
				StringBuilder sb = new StringBuilder();
				for (String s : store) {
					sb.append(s).append('\n');
				}
				log.finer("Cookies : " + sb.toString());
			}
			log.fine("GlobalCookieHandler get called for " + uri.getHost() + " : " + (store != null ? store.size() : "null") + " items returned.");
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public void put(URI uri, Map<String, List<String>> responseHeaders)
			throws IOException {
		List<String> newCookies = responseHeaders.get("Set-Cookie");
		if (newCookies != null) {
			synchronized (cookies) {
				Set<String> store = cookies.get(uri.getHost());
				log.fine("GlobalCookieHandler set called for " + uri.getHost() + " : " + (store != null ? store.size() + " items added." : "no cookie added."));
				if (store != null) {
					StringBuilder sb = new StringBuilder();
					for (String s : store) {
						sb.append(s).append('\n');
					}
					log.finer("Cookies : " + sb.toString());
				}

				if (store == null) {
					store = new HashSet<>();
					cookies.put(uri.getHost(), store);
				}
				store.addAll(newCookies);
			}
		}
	}

	public void setSessionCookie(String host, String key) {
		prefs.put("sessionHost", sessionHost = host);
		prefs.put("sessionCookie", sessionCookie = key);
	}

	public void save() {
		if (sessionHost != null && sessionCookie != null && prefs.getBoolean("saveSession", true)) {
			Set<String> set = cookies.get(sessionHost);
			if (set != null)
				for (String s : set) {
					if (s.startsWith(sessionCookie)) {
						prefs.put("session", s);
						log.finer("Saving cookie : " + s);
					}
				}
		}
	}
}
