package com.example.finalproject.service;

import com.example.finalproject.domain.SocialLoginType;
import com.example.finalproject.social.SocialOauth;
import com.fasterxml.jackson.core.JsonParser;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import org.json.simple.parser.JSONParser;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {
    private final List<SocialOauth> socialOauthList;
    private final HttpServletResponse response;

    @Value("${sns.google.url}")
    private String GOOGLE_SNS_BASE_URL;
    @Value("${sns.google.client.id}")
    private String CLIENT_ID;
    @Value("${sns.google.callback.url}")
    private String REDIRECT_URI;
    @Value("${sns.google.client.secret}")
    private String CLIENT_SECRET;
    @Value("${sns.google.token.url}")
    private String ACCESS_TOKEN_URL;

    public void request(SocialLoginType socialLoginType) {
        SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
        String redirectURL = socialOauth.getOauthRedirectURL();
        try {
            response.sendRedirect(redirectURL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String requestAccessToken(SocialLoginType socialLoginType, String code) {
        SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
        return socialOauth.requestAccessToken(code);
    }


//    public String getGoogleAccessToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
//
//        String code = request.getParameter("code");
//
//        HttpHeaders headers = new HttpHeaders();
//        RestTemplate restTemplate = new RestTemplate();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
//        parameters.add("code", code);
//        parameters.add("client_id", CLIENT_ID);
//        parameters.add("client_secret", CLIENT_SECRET);
//        parameters.add("redirect_uri", REDIRECT_URI);
//        parameters.add("grant_type", GRANT_TYPE);
//
//        HttpEntity<MultiValueMap<String, String>> rest_request = new HttpEntity<>(parameters, headers);
//
//        URI uri = URI.create(ACCESS_TOKEN_URL);
//
//        ResponseEntity<String> responseEntity;
//        responseEntity = restTemplate.postForEntity(uri, rest_request, String.class);
//        String bodys = responseEntity.getBody();
//        log.info("## BODYS = {}", bodys);
//
//        JsonParser jsonParser = new JsonParser();
//        JsonElement jsonElement = jsonParser.parse(bodys);
//
//        return jsonElement.getAsJsonObject().get("access_token").getAsString();
//    }

    public void getGoogleUserInfo(String accessToken) throws Exception {

        //요청하는 클라이언트마다 가진 정보가 다를 수 있기에 HashMap 선언
        HashMap<String, Object> googleUserInfo = new HashMap<>();

        String reqURL = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken;
        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //요청에 필요한 Header에 포함될 내용
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            log.info("## ResponseCode : {}", responseCode);

            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String line = "";
                String result = "";
                while ((line = br.readLine()) != null) {
                    result += line;
                }

                JSONParser parser = new JSONParser();
                log.info("## Result = {}", result);

                JsonElement element = (JsonElement) parser.parse(result);
                String name = element.getAsJsonObject().get("name").getAsString();
                String email = element.getAsJsonObject().get("email").getAsString();
                String id = "GOOGLE_" + element.getAsJsonObject().get("id").getAsString();

                googleUserInfo.put("name", name);
                googleUserInfo.put("email", email);
                googleUserInfo.put("id", id);

                log.info("## Login Controller : {}", googleUserInfo);
            }
        } catch (Exception e) {
            log.info(e.toString());
            throw e;
        }

    }


    private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다."));
    }
}