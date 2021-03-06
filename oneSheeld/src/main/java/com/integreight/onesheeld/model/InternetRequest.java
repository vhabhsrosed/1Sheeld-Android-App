package com.integreight.onesheeld.model;

import com.integreight.onesheeld.shields.controller.InternetShield;
import com.integreight.onesheeld.shields.controller.utils.InternetManager;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.snappydb.SnappydbException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Saad on 1/26/15.
 */
public class InternetRequest {
    private String url;
    private int id;
    private boolean isCancelled = false;
    private REQUEST_STATUS status;
    private AsyncHttpResponseHandler mCallback;
    private InternetShield.CallBack shieldCallback;
    private Map<String, String> headers;
    private Map<String, String> params;
    private String contentType;
    private boolean isIgnored = false;
    private ArrayList<String> registeredCallbacks;
    private String entity = null;
    private String encoding = null;

    public InternetRequest() {
        status = REQUEST_STATUS.IN_QUEUE;
        mCallback = null;
        registeredCallbacks = new ArrayList<>();
        headers = new HashMap<>();
        params = new HashMap<>();
        shieldCallback = new InternetShield.CallBack() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody, int RequestID) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error, int RequestID) {

            }

            @Override
            public void onFinish(int requestID) {

            }

            @Override
            public void onStart(int requestID) {

            }
        };
        setCallback(shieldCallback);
    }

    public InternetRequest(String url, int id, InternetShield.CallBack callback) {
        this();
        this.url = url;
        this.id = id;
        this.shieldCallback = callback;
        setCallback(shieldCallback);
        this.headers = new HashMap<>();
        this.params = new HashMap<>();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        if (url != null) {
//            this.url = this.url.replace(" ", "");
            this.url = this.url.replace("\\", "/");
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public REQUEST_STATUS getStatus() {
        return status;
    }

    public void setStatus(REQUEST_STATUS status) {
        this.status = status;
    }

    public AsyncHttpResponseHandler getCallback() {
        return mCallback;
    }

    public void setCallback(final InternetShield.CallBack callback) {
        this.shieldCallback = new InternetShield.CallBack() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody, int RequestID) {
                if (callback != null)
                    callback.onSuccess(statusCode, headers, responseBody, RequestID);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error, int requestID) {
                if (callback != null)
                    callback.onFailure(statusCode, headers, responseBody, error, requestID);
            }

            @Override
            public void onFinish(int requestID) {
                if (callback != null)
                    callback.onFinish(requestID);
            }

            @Override
            public void onStart(int requestID) {
                if (callback != null)
                    callback.onStart(requestID);
            }
        };
        this.mCallback = new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                status = REQUEST_STATUS.SENT;
                if (!isIgnored && registeredCallbacks.contains(CALLBACK.ON_START.name()))
                    shieldCallback.onStart(id);
                isIgnored = false;
                super.onStart();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                status = REQUEST_STATUS.EXECUTED;
                setResponse(new InternetResponse(responseBody, statusCode, InternetResponse.RESPONSE_STATUS.SUCCESSFUL, headers));
                if (!isIgnored && registeredCallbacks.contains(CALLBACK.ON_SUCCESS.name()))
                    shieldCallback.onSuccess(statusCode, headers, responseBody, id);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                status = REQUEST_STATUS.EXECUTED;
                setResponse(new InternetResponse(responseBody, statusCode, InternetResponse.RESPONSE_STATUS.FAILURE, headers));
                if (!isIgnored && registeredCallbacks.contains(CALLBACK.ON_FAILURE.name()))
                    shieldCallback.onFailure(statusCode, headers, responseBody, error, id);
            }

            @Override
            public void onFinish() {
                status = REQUEST_STATUS.EXECUTED;
                if (!isIgnored && registeredCallbacks.contains(CALLBACK.ON_FINISH.name()))
                    shieldCallback.onFinish(id);
                isIgnored = false;
                super.onFinish();
            }
        };
    }

    public InternetResponse getResponse() {
        try {
            return InternetManager.getInstance().getCachDB().get(id + "", InternetResponse.class);
        } catch (SnappydbException e) {
            return null;
        }
    }

    public void setResponse(InternetResponse response) {
        try {
            InternetManager.getInstance().getCachDB().put(id + "", response);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    public void addHeader(String key, String value) {
//        try {
//            headers.put(URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(value, "UTF-8"));
//        } catch (UnsupportedEncodingException e) {
        headers.put(key, value);
//            e.printStackTrace();
//        }
    }

    public void removeHeader(String key) {
        if (headers.get(key) != null)
            headers.remove(key);
    }

    public void removeAllHeaders() {
        if (headers != null)
            headers.clear();
        headers = new HashMap<>();
    }

    public void addParam(String key, String value) {
        params.put(key, value);
    }

    public void removeParam(String key) {
        if (params.get(key) != null)
            params.remove(key);
    }

    public void removeAllParams() {
        params = new HashMap<>();
        entity = null;
    }

    public Header[] getHeaders() {
        if (headers == null || headers.size() == 0)
            return null;
        Header[] returnedHeaders = new Header[headers.keySet().size()];
        int i = 0;
        for (final String key : headers.keySet()) {
            returnedHeaders[i] = new Header() {
                @Override
                public String getName() {
                    return key;
                }

                @Override
                public String getValue() {
                    return headers.get(key);
                }

                @Override
                public HeaderElement[] getElements() throws ParseException {
                    return new HeaderElement[0];
                }
            };
            i++;
        }
        return returnedHeaders;
    }

    public RequestParams getParams() {
        RequestParams paramsI = new RequestParams();
        if (getEncoding() != null)
            paramsI.setContentEncoding(getEncoding());
        for (final String key : params.keySet()) {
            paramsI.add(key, params.get(key));
        }
        return paramsI;
    }

    public HashMap<String, String> getParamsAsMap() {
        return new HashMap<>(params);
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public HashMap<String, String> getHeadersAsMap() {
        return new HashMap<>(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void ignoreResponse() {
        isIgnored = true;
    }

    public void addRegisteredCallbacks(CALLBACK... callbacks) {
        if (registeredCallbacks == null)
            registeredCallbacks = new ArrayList<>();
        for (CALLBACK callback : callbacks)
            registeredCallbacks.add(callback.name());
    }

    public void setCancelled() {
        this.isCancelled = true;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public ArrayList<String> getRegisteredCallbacks() {
        return registeredCallbacks;
    }

    public enum CALLBACK {
        ON_SUCCESS, ON_FAILURE, ON_START, ON_FINISH, ON_PROGRESS
    }

    public static enum REQUEST_STATUS {
        CALLED, SENT, IN_QUEUE, EXECUTED
    }

}
