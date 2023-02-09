package com.example.OAuth.Controller;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class RedirectController {
    @Context
    HttpHeaders httpHeaders;
    @Context
    UriInfo uriInfo;

    @GetMapping("/redirect")
    public String redirect() {
        JSONObject object = new JSONObject();
        JSONObject headers = new JSONObject();
        JSONObject qp = new JSONObject();
        String json = "error!";
        try {
            for (Map.Entry<String, List<String>> entry : httpHeaders.getRequestHeaders().entrySet()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
            object.put("headers", headers);
            for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
                qp.put(entry.getKey(), entry.getValue().get(0));
            }
            object.put("queryParameters", qp);
            json = object.toString(4);
        } catch (JSONException ex) {
            Logger.getLogger(RedirectController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json;
    }
}
