package com.gotogames.common.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class NetTools {
	
	/**
	 * Open the url and return the content of response (only the first line !)
	 * @param url
	 * @param timeout
	 * @return the content of response or null if url not valid
	 */
	public static String getURLContent(String url, int timeout) {
        Logger log = LogManager.getLogger(NetTools.class);
		String response = null;
		BufferedReader br = null;
		try {
			URL u = new URL(url);
			URLConnection uc = u.openConnection();
			if (timeout > 0) {
				uc.setConnectTimeout(timeout);
				uc.setReadTimeout(timeout);
			}
			br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			response = br.readLine();
			return response;
		}
		catch (MalformedURLException e) {
			log.error("MalformedURLException : "+e.getMessage(), e);
		} catch (SocketTimeoutException e) {
			log.error("SocketTimeoutException : "+e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException : "+e.getMessage(), e);
		} catch (Exception e) {
			log.error("Exception : "+e.getMessage(), e);
		}
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("IOException : "+e.getMessage(), e);
				}
			}
		}
		
		return response;
	}
	
	/**
	 * Open the url and return the content of response (nb line read)
	 * @param url
	 * @param timeout
	 * @param nbLineToRead
	 * @return the content of response or null if url not valid
	 */
	public static String getURLContent(String url, int timeout, int nbLineToRead) {
        Logger log = LogManager.getLogger(NetTools.class);
		String response = null;
		BufferedReader br = null;
		try {
			URL u = new URL(url);
			URLConnection uc = u.openConnection();
			if (timeout > 0) {
				uc.setConnectTimeout(timeout);
				uc.setReadTimeout(timeout);
			}
			br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			int nbLineRead = 0;
			StringBuffer sb = new StringBuffer();
			while (nbLineRead < nbLineToRead) {
				String line = br.readLine();
				if (line == null) break;
				sb.append(line);
			}
			return sb.toString();
		}
		catch (MalformedURLException e) {
			log.error("MalformedURLException : "+e.getMessage(), e);
		} catch (SocketTimeoutException e) {
			log.error("SocketTimeoutException : "+e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException : "+e.getMessage(), e);
		} catch (Exception e) {
			log.error("Exception : "+e.getMessage(), e);
		}
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("IOException : "+e.getMessage(), e);
				}
			}
		}
		
		return response;
	}
	
	/**
	 * Open the url and return all the content of response. Each line is an element in the returned list
	 * @param url
	 * @param timeout
	 * @return
	 */
	public static List<String> getURLContentAllList(String url, int timeout) {
        Logger log = LogManager.getLogger(NetTools.class);
		List<String> response = new ArrayList<String>();
		BufferedReader br = null;
		try {
			URL u = new URL(url);
			URLConnection uc = u.openConnection();
			if (timeout > 0) {
				uc.setConnectTimeout(timeout);
				uc.setReadTimeout(timeout);
			}
			br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				response.add(line);
			}
			return response;
		}
		catch (MalformedURLException e) {
			log.error("MalformedURLException : "+e.getMessage(), e);
		} catch (SocketTimeoutException e) {
			log.error("SocketTimeoutException : "+e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException : "+e.getMessage(), e);
		} catch (Exception e) {
			log.error("Exception : "+e.getMessage(), e);
		}
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("IOException : "+e.getMessage(), e);
				}
			}
		}
		
		return response;
	}
	
	/**
	 * Open the url and return all the content of response
	 * @param url
	 * @param timeout
	 * @return the content of response or null if url not valid
	 */
	public static String getURLContentAll(String url, int timeout) {
        Logger log = LogManager.getLogger(NetTools.class);
		String response = null;
		BufferedReader br = null;
		try {
			URL u = new URL(url);
			URLConnection uc = u.openConnection();
			if (timeout > 0) {
				uc.setConnectTimeout(timeout);
				uc.setReadTimeout(timeout);
			}
			br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		}
		catch (MalformedURLException e) {
			log.error("MalformedURLException : "+e.getMessage(), e);
		} catch (SocketTimeoutException e) {
			log.error("SocketTimeoutException : "+e.getMessage(), e);
		} catch (IOException e) {
			log.error("IOException : "+e.getMessage(), e);
		} catch (Exception e) {
			log.error("Exception : "+e.getMessage(), e);
		}
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("IOException : "+e.getMessage(), e);
				}
			}
		}
		
		return response;
	}
}
