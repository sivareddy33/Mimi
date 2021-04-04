package com.leagueofshadows.enc.REST;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


class VolleyHelper {

    private static VolleyHelper mInstance;
    private RequestQueue mRequestQueue;
    private Context mCtx;

    private VolleyHelper(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    static synchronized VolleyHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleyHelper (context);
        }
        return mInstance;
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
    void cancel(final String tag)
    {
        mRequestQueue.cancelAll(tag);
    }

}

