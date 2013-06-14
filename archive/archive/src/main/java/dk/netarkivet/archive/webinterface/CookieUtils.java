/**
 * 
 */
package dk.netarkivet.archive.webinterface;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ngiraud
 *
 */
public class CookieUtils {

	/**
	 * Some cookie lifespan to play with.
	 *
	 */
	public static enum Lifespan {
	
		MINUTE(60),
		HOUR(60 * 60),
		DAY(24 * 60 * 60),
		WEEK(7 * 24 * 60 * 60);
		
		private final int seconds;

		private Lifespan(int seconds) {
			this.seconds = seconds;
		}

		public int getSeconds() {
			return seconds;
		}	
		
	}
	
	/**
	 * Returns the value of a request parameter, or if not found 
	 * tries to find a cookie with the same name.
	 * @param request the HTTP request
	 * @param name the parameter name
	 * @return the value (never null, may be empty)
	 */
	public static final String getParameterValue(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		if (value == null || value.isEmpty()) {
			for (Cookie c : request.getCookies()) {
				if (name.equals(c.getName())) {
					value = c.getValue();
				}
			}
		}
		return (value != null ? value : "");
	}
	
	/**
	 * Set a cookie on the client.
	 * @param response
	 * @param name
	 * @param value
	 * @param lifeSpan
	 */
	public static final void setCookie(
			HttpServletResponse response,
			String name, 
			String value, 
			Lifespan lifeSpan) {
		Cookie c = new Cookie(name, value);
		c.setMaxAge(lifeSpan.getSeconds());
		response.addCookie(c);
	}
	
	/**
	 * Set a cookie on the client, with a default lifespan of @see Lifespan#HOUR
	 * @param response the HTTP response
	 * @param name
	 * @param value
	 */
	public static final void setCookie(
			HttpServletResponse response,
			String name, 
			String value) {setCookie(response, name, value, Lifespan.HOUR);
	}

}
