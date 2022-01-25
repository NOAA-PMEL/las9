package pmel.sdig.las;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LASProxy {
	public int streamBufferSize = 8196;
	private static Logger log = LoggerFactory.getLogger(LASProxy.class.getName());
	Gson gson = new Gson();
	public void executeERDDAPMethodAndSaveResult(String url, File outfile, HttpServletResponse response) throws IOException, HttpException {
		int timeout = 600;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.setCircularRedirectsAllowed(true).build();
		CloseableHttpClient client =
				HttpClientBuilder.create().setDefaultRequestConfig(config).build();

		HttpGet method = new HttpGet(url);
		method.setConfig(config);
		String message = "An error occurred downloading the data file from ERDDAP.";
		try {

			HttpResponse httpResponse = client.execute(method);
			int rc = httpResponse.getStatusLine().getStatusCode();

			if (rc != HttpStatus.SC_OK) {
				message = EntityUtils.toString(httpResponse.getEntity());
				if ( message.startsWith("<!doc")) {
					int start = message.indexOf("Message")+11;
					message = message.substring(start);
					int end = message.indexOf("</p>");
					message = message.substring(0, end);
					throw new IOException(message);
				} else {
					ErddapError err = gson.fromJson(message, ErddapError.class);
					log.error(err.getMessage());
					throw new IOException(err.getMessage());
				}
			}
			InputStream input = httpResponse.getEntity().getContent();
			OutputStream output = new FileOutputStream(outfile);
			stream(input, output);
			method.releaseConnection();
		} catch (Exception e) {
			log.error("Trouble downloading ERDDAP data set." + e.getMessage());
			method.releaseConnection();
			throw new IOException(message);
		}
		method.releaseConnection();
	}
	public class ErddapError {
		/*
		Error {
           code=404;
           message="Not Found: Your query produced no matching results. (time>=2019-03-27T00:00:00Z is outside of the variable's actual_range: 1970-02-26T20:00:00Z to 2019-03-26T15:00:00Z)";
        }
		*/
		int code;
		String message;

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
	public void executeGetMethodAndSaveResult(String url, File outfile, HttpServletResponse response) throws IOException, HttpException {
		int timeout = 120;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.setCircularRedirectsAllowed(true).build();
		CloseableHttpClient client =
				HttpClientBuilder.create().setDefaultRequestConfig(config).build();

		HttpGet method = new HttpGet(url);
		method.setConfig(config);
		try {

			HttpResponse httpResponse = client.execute(method);
			int rc = httpResponse.getStatusLine().getStatusCode();

			if (rc != HttpStatus.SC_OK) {

				log.error("HttpGet Error Code: "+rc);
				if ( response == null ) {
					throw new IOException("HttpGet Error Code: "+rc);
				} else {
					response.sendError(rc);
				}
			} 
			InputStream input = httpResponse.getEntity().getContent();
			OutputStream output = new FileOutputStream(outfile);
			stream(input, output);
		} finally {

			method.releaseConnection();
		}
		

	}
	public String executeGetMethodAndReturnResult(String url, HttpServletResponse response) throws IOException, HttpException {

		int timeout = 15;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.setCircularRedirectsAllowed(true).build();
		CloseableHttpClient client =
				HttpClientBuilder.create().setDefaultRequestConfig(config).build();

		HttpGet method = new HttpGet(url);
		method.setConfig(config);

		try {

			HttpResponse httpResponse = client.execute(method);
			int rc = httpResponse.getStatusLine().getStatusCode();

			if (rc != HttpStatus.SC_OK) {

				log.error("HttpGet Error Code: "+rc);
                if ( response == null ) {
                	throw new IOException("Failed RC="+rc + " on URL=" + url);
                } else {
				    log.error("Error getting data from " + url + " http status code: " + rc);
                }

				return null;

			}
			return EntityUtils.toString(httpResponse.getEntity());
		} catch (Exception e) {
			log.error("Error getting data from " + url + " " + e.getMessage());
			return null;
		}
		finally {
			method.releaseConnection();
		}
	}
	public String executeGetMethodAndReturnResult(String url) throws HttpException, IOException {
		return executeGetMethodAndReturnResult(url, null);
	}
	   /**
  * Makes HTTP GET request and writes result to response output stream.
  * @param request fully qualified request URL.
  * @param response the response
  * @throws IOException
  * @throws HttpException
  */
	public InputStream executeGetMethodAndReturnStream(String request, HttpServletResponse response, int timeout) throws IOException, HttpException {

		HttpClient client = HttpClientBuilder.create().build();
		RequestConfig config =  RequestConfig.custom().setCircularRedirectsAllowed(true).setConnectionRequestTimeout(timeout*1000).setConnectTimeout(timeout*1000).build();
		HttpGet method = new HttpGet(request);
		method.setConfig(config);

	    method.setHeader("Connection", "close");

	    HttpResponse httpResponse = client.execute(method);
		int rc = httpResponse.getStatusLine().getStatusCode();

	    if (rc != HttpStatus.SC_OK) {

	        log.error("HttpGet Error Code: "+rc);

	        //response.sendError(rc);

	        return null;
	    }
	    return httpResponse.getEntity().getContent();

	}
	   /**
     * Makes HTTP GET request and writes result to response output stream.
     * @param request fully qualified request URL.
     * @param response the response
     * @throws IOException
     * @throws HttpException
     */
	public InputStream executeGetMethodAndReturnStream(String request, HttpServletResponse response) throws IOException, HttpException {

		return executeGetMethodAndReturnStream(request, response, -1);
	   
	}

	/**
	 * Makes HTTP GET request and writes result to response output stream.
	 * @param request fully qualified request URL.
	 * @param response the response
	 * @throws IOException
	 * @throws HttpException
	 */
	public void executeGetMethodAndStreamResult(String request, HttpServletResponse response) throws IOException, HttpException {
		int timeout = 15;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.setCircularRedirectsAllowed(true).build();
		CloseableHttpClient client =
				HttpClientBuilder.create().setDefaultRequestConfig(config).build();

		HttpGet method = new HttpGet(request);
		method.setConfig(config);
		method.setHeader("Connection", "close");

		try {

			HttpResponse httpResponse = client.execute(method);
			int rc = httpResponse.getStatusLine().getStatusCode();
			
			if (rc != HttpStatus.SC_OK) {

				log.error("HttpGet Error Code: "+rc);

				response.sendError(rc);

				return;
			}

			streamGetMethodResponse(httpResponse, response.getOutputStream());
		}
		finally {

			method.releaseConnection();
		}
	}
	/**
	 * Makes HTTP GET request and writes result to response output stream.
	 * @param request fully qualified request URL.
	 * @param output OutputStream to write to
	 * @throws IOException
	 * @throws HttpException
	 */
	public void executeGetMethodAndStreamResult(String request, OutputStream output) throws IOException, HttpException {

		int timeout = 15;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.setCircularRedirectsAllowed(true).build();
		CloseableHttpClient client =
				HttpClientBuilder.create().setDefaultRequestConfig(config).build();

		HttpGet method = new HttpGet(request);
		method.setConfig(config);
		method.setHeader("Connection", "close");

		try {

			HttpResponse httpResponse = client.execute(method);
			int rc = httpResponse.getStatusLine().getStatusCode();

			if (rc != HttpStatus.SC_OK) {

				log.error("HttpGet Error Code: "+rc);

				HttpException exception = new HttpException("HTTP Get returned error: " + rc);

				throw exception;
			}

			streamGetMethodResponse(httpResponse,output);
		}
		finally {

			method.releaseConnection();
		}

	}

	public void streamGetMethodResponse(HttpResponse method, OutputStream output) throws IOException, HttpException {

		InputStream input = method.getEntity().getContent();
		stream(input, output);

	}

	public void stream(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buffer = new byte[streamBufferSize];
			int count = input.read(buffer);

			while( count != -1 && count <= streamBufferSize ) {

				output.write(buffer,0,count);
				count = input.read(buffer);
			}

			input.close();
			output.close();
		} catch (IOException e) {
			throw e;
		} finally {
			input.close();
			output.close();
		}
	}
	
	HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {

	    public boolean retryRequest(
	            IOException exception,
	            int executionCount,
	            HttpContext context) {
	        if (executionCount >= 5) {
	            // Do not retry if over max retry count
	            return false;
	        }
	        if (exception instanceof InterruptedIOException) {
	            // Timeout
	            return false;
	        }
	        if (exception instanceof UnknownHostException) {
	            // Unknown host
	            return false;
	        }
	        if (exception instanceof ConnectTimeoutException) {
	            // Connection refused
	            return false;
	        }
	        if (exception instanceof SSLException) {
	            // SSL handshake exception
	            return false;
	        }
	        HttpClientContext clientContext = HttpClientContext.adapt(context);
	        HttpRequest request = clientContext.getRequest();
	        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
	        if (idempotent) {
	            // Retry if the request is considered idempotent
	            return true;
	        }
	        return false;
	    }

	};
	public static String makeHTTPostRequestWithHeaders(String connectToURL, List<NameValuePair> keys, String postData) {
		try{
			HttpClient httpClient=new DefaultHttpClient();
			HttpPost httpPost=new HttpPost(connectToURL);
			if(keys!=null){
				
				for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
					NameValuePair nameValuePair = (NameValuePair) iterator.next();			
					httpPost.addHeader(nameValuePair.getName(), nameValuePair.getValue() );
				}
			}
			httpPost.addHeader("Content-Type","application/x-www-form-urlencoded ");
			httpPost.setEntity(new StringEntity(postData));
			HttpResponse httpResponse=httpClient.execute(httpPost);
			return EntityUtils.toString(httpResponse.getEntity(),"utf-8");

		}catch(Exception e){
			log.debug("Post failed: "+e.getMessage());
		}
		return null;

	}
}
