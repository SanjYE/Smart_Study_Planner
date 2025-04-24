package com.example;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeminiClient {

    private static final String API_KEY = "AIzaSyD6C1dYpg-k_ZLiaPuonL9OrCkr5UKjsfw"; // <-- REPLACE WITH YOUR ACTUAL API KEY
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash";

    public static void main(String[] args) {
        try {
            String prompt = "Explain the concept of a smart study planner in 30 words.";
            String response = generateText(prompt);
            System.out.println("Gemini says:\n" + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generateText(String prompt) throws IOException {
        String url = BASE_URL + ":generateContent?key=" + API_KEY;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");

            // Construct the request body properly using Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();
            ArrayNode contentsArray = objectMapper.createArrayNode();
            ObjectNode contentItem = objectMapper.createObjectNode();
            ArrayNode partsArray = objectMapper.createArrayNode();
            ObjectNode textPart = objectMapper.createObjectNode();
            
            // Set the text without manual string formatting
            textPart.put("text", prompt);
            partsArray.add(textPart);
            contentItem.set("parts", partsArray);
            contentsArray.add(contentItem);
            rootNode.set("contents", contentsArray);
            
            String requestBody = objectMapper.writeValueAsString(rootNode);
            
            // Print the prompt for debugging
            System.out.println("Sending prompt to Gemini API: " + prompt);
            System.out.println("Request body: " + requestBody);
            
            httpPost.setEntity(new StringEntity(requestBody, "UTF-8"));

            // Execute the request
            HttpResponse response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            
            // Print the raw response for debugging
            System.out.println("Raw API response: " + jsonResponse);

            // Parse the JSON response
            JsonNode responseRootNode = objectMapper.readTree(jsonResponse);

            // Extract the generated text
            if (responseRootNode.has("candidates") && 
                responseRootNode.get("candidates").isArray() && 
                responseRootNode.get("candidates").size() > 0 && 
                responseRootNode.get("candidates").get(0).has("content") && 
                responseRootNode.get("candidates").get(0).get("content").has("parts") && 
                responseRootNode.get("candidates").get(0).get("content").get("parts").isArray() &&
                responseRootNode.get("candidates").get(0).get("content").get("parts").size() > 0 &&
                responseRootNode.get("candidates").get(0).get("content").get("parts").get(0).has("text")) {
                
                return responseRootNode.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
            } else if (responseRootNode.has("error")) {
                return "Error: " + responseRootNode.get("error").get("message").asText();
            } else {
                return "No response or unexpected format. Response: " + jsonResponse;
            }
        }
    }
}