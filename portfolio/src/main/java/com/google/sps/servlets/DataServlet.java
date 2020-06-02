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
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private final ArrayList<UserComment> userComments = new ArrayList<>();

  /*
   * Called when a client submits a GET request to the /data URL
   * Displays all recorded user comments on page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Comment").addSort("time", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    ArrayList<UserComment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
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
    
    // Gson gson = new Gson();
    // String json = gson.toJson(userComments);
    // response.setContentType("application/json;");
    // response.getWriter().println(json);
  }

  /*
   * Called when a client submits a POST request to the /data URL
   * Adds submitted comment to internal record if the comment is
   * non-empty. Then, the page is reloaded.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String userComment = getFieldFromResponse(request, "comment", "");
    if(userComment.length() != 0) {
      String userName = getFieldFromResponse(request, "name", "Anonymous");
      String userEmail = getFieldFromResponse(request, "email", "janedoe@gmail.com");
      Date date = new Date();
      String currDate = date.toString();
      String userDate = getFieldFromResponse(request, "time", currDate);
      addToDatastore(userName, userEmail, userDate, userComment);
      UserComment comment = UserComment.create(userName, userEmail, userComment, userDate);
      userComments.add(comment);
    }
    response.sendRedirect("index.html");
  }

  /*
   * Extracts the value of fieldName attribute from request object if present
   * and returns defaultValue if it is not
   */
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

  private void addToDatastore(String name, String email, String dateTime, String comment) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("email", email);
    commentEntity.setProperty("time", dateTime);
    commentEntity.setProperty("comment", comment);
    datastore.put(commentEntity);
  }
}
