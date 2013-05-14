package com.xively.android.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;
import android.util.Log;

import com.xively.android.service.exception.ParseResponseException;
import com.xively.android.service.exception.RequestUnsuccessfulException;

/**
 * {@link AsyncTask} for making the http request.
 * 
 * @author s0pau
 */
public class HttpTask extends AsyncTask<Request, Integer, Response[]>
{
	private static final String TAG = HttpTask.class.getSimpleName();

	@Override
	protected Response[] doInBackground(Request... requests)
	{
		Response[] responses = new Response[requests.length];
		for (int i = 0; i < requests.length; i++)
		{
			responses[i] = null;
			try
			{
				responses[i] = makeRequest(requests[i]);
			} catch (RequestUnsuccessfulException e)
			{
				String msg = "Request was not made successfully";
				Log.w(TAG, msg, e);
				responses[i] = ResponseHelper.writeException(msg, e);
			} catch (ParseResponseException e)
			{
				String msg = "Response cannot be parsed successfully";
				Log.w(TAG, msg, e);
				responses[i] = ResponseHelper.writeException(msg, e);
			}

			publishProgress((i / requests.length) * 100);

			if (isCancelled())
			{
				// break if cancelled
				break;
			}
		}

		return responses;
	}

	private Response makeRequest(Request request)
	{
		Log.d(TAG, String.format("request(%s, %s)", request.getUriStr(), request.getBody()));

		Response retval = null;

		URL url;
		try
		{
			url = new URL(request.getUriStr());
		} catch (MalformedURLException e)
		{
			throw new RequestUnsuccessfulException("Cannot make request with malformed URL.", e);
		}

		try
		{
			URLConnection conn = url.openConnection();

			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);

			httpConn.setRequestMethod(request.getHttpMethod());
			httpConn.setRequestProperty("X-ApiKey", request.getApiKey());
			httpConn.setRequestProperty("Content-Type", "application/json");
			// FIXME
			// httpConn.setRequestProperty("Accept-Encoding", "gzip");

			httpConn.connect();

			Log.d(TAG, String.format("Response on request: %s", httpConn.getResponseMessage()));
			String content = ResponseHelper.read(httpConn.getInputStream());
			retval = new Response(httpConn.getResponseCode(), httpConn.getResponseMessage(), content);
		} catch (IOException e)
		{
			throw new RequestUnsuccessfulException("Unable to open connection for request.", e);
		}

		return retval;
	}
}
