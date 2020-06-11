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
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery.Builder;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.text.DateFormat; 
import java.text.SimpleDateFormat; 
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/chart")
public class ChartServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /chart URL
   * Prepares data about the number of comments each day and submits
   * it to the client for rendering
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    /*
     * This data structure maps a date string in the format (06-31-2020) to a DayComments object
     * with 2 attributes - the number of root comments on this day, and the number of replies
     * on this day
     */
    HashMap<String, DayComments> numCommentsOnDay = new HashMap<>();

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query = Query.newEntityQueryBuilder().setKind("Comment").build();
    QueryResults<Entity> results = datastore.run(query);

    while (results.hasNext()) {
      Entity comment = results.next();

      long dateTime = comment.getLong("time");
      Date date = new Date(dateTime);
      String dateString = dateFormat.format(date);

      DayComments prevEntry;
      if (numCommentsOnDay.containsKey(dateString)) {
        prevEntry = numCommentsOnDay.get(dateString);
      } else {
        prevEntry = DayComments.create(0, 0);
      }

      DayComments thisDayComments;
      long rootId = comment.getLong("rootid");
      if (rootId == 0) {
        // If this comment is a root comment, increase the number of root comments today by one
        thisDayComments = DayComments.create(prevEntry.rootComments() + 1, prevEntry.replies());
      } else {
        // Otherwise, increase the number of replies today by one
        thisDayComments = DayComments.create(prevEntry.rootComments(), prevEntry.replies() + 1);
      }
      numCommentsOnDay.put(dateString, thisDayComments);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(numCommentsOnDay));
  }
}
