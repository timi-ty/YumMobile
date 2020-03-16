package com.inc.tracks.yummobile.utils;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class YumJsonObjectRequest extends JsonObjectRequest {

    private final Map<String, String> headers;


    public YumJsonObjectRequest(int method, String url, HashMap<String, String> headers, @Nullable JSONObject jsonRequest,
                                Response.Listener<JSONObject> listener,
                                @Nullable Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);

        this.headers = headers;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }
}
