package eu.neurovertex.yacapd;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author NeuroVertex
 *         Date: 12/09/13, 16:52
 */
public class HTTPSDownloader extends Observable implements HostnameVerifier {

	private YacapPageProcessor processor;
	private boolean exit = false;
	private static final Preferences prefs = Preferences.userNodeForPackage(HTTPSDownloader.class);
	private static final String logFile = "/downloaded_%s.html",
			ENCODING = "UTF-8";
	private static final Logger log = Logger.getLogger(HTTPSDownloader.class.getName());

	static {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true"); // Needed to be able to set the Origin header.
		GlobalCookieHandler.setDefault(new GlobalCookieHandler());
	}

	public HTTPSDownloader(YacapPageProcessor processor) {
		this.processor = processor;
	}

	public int getUrl(YacapPageProcessor.HTTPRequest request, String logName) {
		return downloadUrl(request.getUrl(), logName, "GET", request.getParameters(), null, 0);
	}

	public int getUrl(String urlString, String logName, Map<String, String> requestHeaders) {
		return downloadUrl(urlString, logName, "GET", requestHeaders, null, 0);
	}

	public int postUrl(YacapPageProcessor.HTTPRequest request, String logName) {
		return downloadUrl(request.getUrl(), logName, "POST", request.getParameters(), encodeData(request.getData()), 0);
	}

	public int postUrl(String urlString, String logName, Map<String, String> requestHeaders, String data) {
		return downloadUrl(urlString, logName, "POST", requestHeaders, data, 0);
	}

	private int downloadUrl(String urlString, String logName, String method, Map<String, String> requestHeaders, String data, int retries) {
		HttpsURLConnection connection;
		notifyObservers(new DownloaderStatus(urlString, "Conneting", null, 0));
		try {
			URL url = new URL(urlString);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier(this);
			connection.setRequestMethod(method);
			// Changes the User Agent. Default is Chrome's
			connection.setRequestProperty("User-Agent", prefs.get("UserAgent", "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1629.2 Safari/537.36"));
			if (requestHeaders != null)
				for (String key : requestHeaders.keySet()) {
					connection.addRequestProperty(key, requestHeaders.get(key));
				}

			connection.setDoInput(true);
			connection.setDoOutput(data != null);
			Map<String, List<String>> requestProperties = connection.getRequestProperties();


			log.fine("Downloading (" + method + ") " + url.toExternalForm());
			if (data != null) {
				DataOutputStream out = new DataOutputStream(connection.getOutputStream());
				out.writeBytes(data);
				out.flush();
				out.close();
			} else
				connection.connect();

			int code = connection.getResponseCode();
			Document doc = null;
			try {
				doc = Jsoup.parse(connection.getInputStream(), null, url.toExternalForm());
				connection.getInputStream().close();
			} catch (IOException e) {
				log.severe("Got error while downloading " + urlString + " : " + e.getMessage());
			}
			if (code == 200) {
				processor.processDocument(doc);
			} else if (code == 302) {
				log.fine("Got 302, redirecting to : " + connection.getHeaderField("Location"));
				if (connection.getHeaderField("Location") != null) {
					return getUrl(connection.getHeaderField("Location"), logName, null);
				}
			}
			connection.disconnect();
			writeLog(logName, doc, url.toExternalForm(), method, code, requestProperties, connection.getHeaderFields(), data);
			notifyObservers(new DownloaderStatus(urlString, "Conneting", null, code));
			return code;
		} catch (MalformedURLException e) {
			log.severe("Error : URL " + urlString + " is malformed.");
			System.exit(-1);
		} catch (ClassCastException e) {
			log.severe("URLConnection to " + urlString + " can't be casted to HTTPS");
			System.exit(-1);
		} catch (IOException e) {
			log.severe("Error while connecting : " + e.getMessage());
			notifyObservers(new DownloaderStatus("Connection timeout", urlString, e, -1));
			try {
				GlobalCookieHandler.getDefault().clearCookies();
				Thread.sleep(prefs.getInt("delay", 3000));
			} catch (InterruptedException ignore) {
			}
			if (!exit)
				return downloadUrl(urlString, logName, method, requestHeaders, data, retries + 1);
		}
		return -1;
	}

	private void writeLog(String logname, Document doc, String url, String method, int code, Map<String, List<String>> requestHeaders, Map<String, List<String>> responseHeaders, String data) throws IOException {
		String logDir = prefs.get("logDir", null);
		if (logDir == null || logDir.isEmpty())
			return;
		String filename = String.format(logFile, logname);
		PrintWriter out = new PrintWriter(new File(filename));
		out.println("<!--Request URL: " + url +
				"\nRequest Method: " + method +
				"\nStatus Code: " + code +
				"\nRequest Headers");
		Map<String, List<String>> headers = requestHeaders;
		StringBuilder sb;
		for (String key : headers.keySet()) {
			sb = new StringBuilder("\t");
			sb.append(key).append(": ");
			int n = 0;
			for (String val : headers.get(key))
				if (val != null)
					sb.append(n++ > 0 ? "," + val : val);
			out.println(sb.toString());
		}
		if (data != null) {
			//out.println("With POST Data"); // Not logging the data for obvious reasons.
			out.println("With POST Data : " + data);
		}
		out.println("\nResponse Headers");
		headers = responseHeaders;
		for (String key : headers.keySet()) {
			sb = new StringBuilder("\t");
			if (key != null)
				sb.append(key).append(": ");
			int n = 0;
			for (String val : headers.get(key))
				sb.append(n++ > 0 ? "," + val : val);
			out.println(sb.toString());
		}
		out.println("\nContent : \n\n-->");
		if (doc != null)
			out.write(doc.outerHtml());
		out.flush();
		out.close();
		log.log((code == 200 ? Level.INFO : Level.SEVERE), "File parsed and printed to logfile " + filename + "(" + url + ")");
	}

	public String encodeData(Map<String, String> data) {
		StringBuilder str = new StringBuilder();
		int n = 0;
		for (String key : data.keySet()) {
			if (n++ > 0)
				str.append('&');
			str.append(key).append('=');
			try {
				str.append(URLEncoder.encode(data.get(key), ENCODING));
			} catch (UnsupportedEncodingException ignore) {
			} // Find a Java implementation that doesn't support UTF-8, I dare you
		}
		return str.toString();
	}

	protected void stop() {
		exit = true;
	}

	@Override
	public boolean verify(String hostname, SSLSession session) {
		// Turns off hostname verifying by accepting anything
		return true;
	}

	public class DownloaderStatus {
		private int httpCode;
		private String status, url;
		private Throwable exception;

		public DownloaderStatus(String status, String url, Throwable exception, int httpCode) {
			this.status = status;
			this.url = url;
			this.exception = exception;
			this.httpCode = httpCode;
		}

		public int getHttpCode() {
			return httpCode;
		}

		public String getStatus() {
			return status;
		}

		public String getUrl() {
			return url;
		}

		public Throwable getException() {
			return exception;
		}
	}

}
