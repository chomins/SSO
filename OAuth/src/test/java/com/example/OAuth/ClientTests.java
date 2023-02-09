package com.example.OAuth;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientTests {

    private String URL = "http://localhost:9999";
    private Client client = ClientBuilder.newClient();
    @Test
    public void authorizationRequest(){
        try {
            Response response= makeAuthCodeRequest();
            String responseEntity = response.readEntity(String.class);
            System.out.println("---------------");
            System.out.println("Auth Code is : "+getCode(responseEntity));
            System.out.println("----------------");
        } catch (OAuthSystemException | URISyntaxException | OAuthProblemException ex) {
            Logger.getLogger(ClientTests.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void authCodeTokenRequest() throws Exception {
        Response response = makeAuthCodeRequest();
        String responseEntity = response.readEntity(String.class);
        String authCode = getCode(responseEntity);
        String token = getAccessToken(makeTokenRequestWithAuthCode(authCode));
        System.out.println("accessToken : "+token);
    }

    @Test
    public void directTokenRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED));
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", Common.CLIENT_ID);
        map.add("client_secret",Common.CLIENT_SECRET);
        map.add("grant_type",GrantType.PASSWORD.toString() );
        map.add("redirect_uri", URL+"/redirect");
        map.add("username", Common.USERNAME);
        map.add("password",Common.PASSWORD);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(URL+"/token", request, String.class);
        System.out.println(response.getBody());
    }

    @Test
    public void endToEndWithAuthCode() throws Exception{
        Response response = makeAuthCodeRequest();
        String responseEntity = response.readEntity(String.class);
        String authCode = getCode(responseEntity);
        String token = getAccessToken(makeTokenRequestWithAuthCode(authCode));
        URL restUrl = new URL(URL + "/resource");
        WebTarget target = client.target(restUrl.toURI());
        Response resp = target.request(MediaType.APPLICATION_JSON)
                .header(Common.HEADER_AUTHORIZATION, "Bearer " + token)
                .get();
        String respBody = resp.readEntity(String.class);
        JSONObject jsonObject = new JSONObject(respBody);
        System.out.println(jsonObject.getString("entity"));
    }


    private Response makeAuthCodeRequest() throws OAuthSystemException, URISyntaxException, OAuthProblemException {
        OAuthClientRequest request = OAuthClientRequest
                .authorizationLocation(URL+"/authorization")
                .setClientId(Common.CLIENT_ID)
                .setRedirectURI(URL+"/redirect")
                .setResponseType(ResponseType.CODE.toString())
                .setState("state")
                .buildQueryMessage();
        WebTarget target = client.target(new URI(request.getLocationUri()));
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
        return response;
    }

    private String makeTokenRequestWithAuthCode(String authCode) throws Exception{
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED));
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", Common.CLIENT_ID);
        map.add("client_secret",Common.CLIENT_SECRET);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", URL+"/redirect");
        map.add("code", authCode);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(URL+"/token", request, String.class);
        return response.getBody();

    }

    public String getCode(String response) throws JSONException, URISyntaxException {
        JSONObject jsonObject = new JSONObject(response);
        String location = jsonObject.getString("location");
        URI uri = new URI(location);
        List<NameValuePair> params = URLEncodedUtils.parse(uri,StandardCharsets.UTF_8);
        for (NameValuePair param : params) {
            if(param.getName().equals("code")){
                return param.getValue();
            }
        }
        return "code not exist";
    }

    public String getAccessToken(String response) throws Exception {
        JSONObject jsonObject = new JSONObject(response);
        String body = jsonObject.getJSONObject("entity").getString("body");
        JSONObject tokenObj = new JSONObject(body);
        return tokenObj.getString("access_token");
    }


}
