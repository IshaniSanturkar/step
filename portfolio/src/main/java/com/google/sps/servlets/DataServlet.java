// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private final ArrayList<UserComment> userComments = new ArrayList<>();
  private final String defaultMaxComment = "20";

  /*
   * Called when a client submits a GET request to the /data URL
   * Displays all recorded user comments on page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int maxCommentDisplay = Integer.parseInt(getFieldFromResponse(request, "maxcomments", defaultMaxComment));
    Query query = new Query("Comment").addSort("time", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    int displayed = 0;
    ArrayList<UserComment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      if(displayed == maxCommentDisplay) {
          break;
      }
      displayed++;
      long id = entity.getKey().getId();
      String name = (String) entity.getProperty("name");
      String email = (String) entity.getProperty("email");
      String time = (String) entity.getProperty("time");
      String comment = (String) entity.getProperty("comment");
      UserComment userComment = UserComment.create(name, email, comment, time);
      comments.add(userComment);
    }

    Gson gson = new Gson();

    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  /*
   * Called when a client submits a POST request to the /data URL
   * Adds submitted comment to internal record if the comment is
   * non-empty. Then, the page is reloaded.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String parsedBody = getBody(request);
    String trimmedBody = parsedBody.substring(1, parsedBody.length() - 1);
    Map<String, String> immutableMap = Splitter.on('&').trimResults().withKeyValueSeparator('=').split(trimmedBody);
    HashMap<String, String> mutableMap = new HashMap<>();
    for (String key : immutableMap.keySet()) {
            String decodedVal = java.net.URLDecoder.decode(immutableMap.get(key), StandardCharsets.UTF_8.name());
            mutableMap.put(key, decodedVal);
    }
    String userComment = getFieldFromMap(mutableMap, "comment", "");
    if(userComment.length() != 0) {
      String userName = getFieldFromMap(mutableMap, "name", "Anonymous");
      String userEmail = getFieldFromMap(mutableMap, "email", "janedoe@gmail.com");
      Date date = new Date();
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); 
      df.setTimeZone(tz);
      String currDate = df.format(date).toString();
      String userDate = getFieldFromMap(mutableMap, "timestamp", currDate);
      addToDatastore(userName, userEmail, userDate, userComment);
      UserComment comment = UserComment.create(userName, userEmail, userComment, userDate);
      userComments.add(comment);
    }
  }


  /*
   * Extracts the value of fieldName attribute from request object if present
   * and returns defaultValue if it is not
   */
  private String getFieldFromMap(Map<String, String> valueMap, String fieldName, String defaultValue) {
    String fieldValue = valueMap.getOrDefault(fieldName, defaultValue);
    if(fieldValue.length() == 0)
    {
        fieldValue = defaultValue;
    }
    return fieldValue;
  }

  private void addToDatastore(String name, String email, String dateTime, String comment) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("email", email);
    commentEntity.setProperty("time", dateTime);
    commentEntity.setProperty("comment", comment);
    datastore.put(commentEntity);
  }

  public static String getBody(HttpServletRequest request) throws IOException {

    String body = null;
    StringBuilder stringBuilder = new StringBuilder();
    BufferedReader bufferedReader = null;

    try {
        InputStream inputStream = request.getInputStream();
        if (inputStream != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            char[] charBuffer = new char[128];
            int bytesRead = -1;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                stringBuilder.append(charBuffer, 0, bytesRead);
            }
        } else {
            stringBuilder.append("");
        }
    } catch (IOException ex) {
        throw ex;
    } finally {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException ex) {
                throw ex;
            }
        }
    }

    body = stringBuilder.toString();
    return body;
}

private String getFieldFromResponse(HttpServletRequest request, String fieldName, String defaultValue) {
        String[] defaultArr = {defaultValue};
        String[] fieldValues = request.getParameterMap().getOrDefault(fieldName, defaultArr);
        if(fieldValues.length > 1) {
        throw new IllegalArgumentException("Found multiple values for single key in form");
        } else {
        String userValue = fieldValues[0];
        if(userValue.length() == 0) {
            return defaultValue;
        } else {
            return userValue;
        }
        }
    }
}



