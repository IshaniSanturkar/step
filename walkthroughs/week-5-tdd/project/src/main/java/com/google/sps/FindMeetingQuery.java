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

package com.google.sps;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class FindMeetingQuery {

  /*
   * Takes in a list of non-overlapping time ranges sorted by start (and end) as well as a new
   * event. Returns a list of non-overlapping time ranges covered by the existing list as well
   * as the given event.
   */
  private ArrayList<TimeRange> addToBusy(ArrayList<TimeRange> busyTimes, Event meeting) {
    TimeRange meetingTime = meeting.getWhen();
    /*
     * Stores the index of the search key, if it is contained in the list; otherwise,
     * (-(insertion point) - 1). The insertion point is defined as the point at which
     * the key would be inserted into the list: the index of the first element greater
     * than the key, or list.size() if all elements in the list are less than the specified key.
     * This variable signifies where the start time of the meeting fits into the list.
     */
    int startLoc = Collections.binarySearch(busyTimes, meetingTime, TimeRange.ORDER_BY_START);
    // The position where the end time of the meeting fits into the sorted list
    int endLoc = Collections.binarySearch(busyTimes, meetingTime, TimeRange.ORDER_BY_END);
    // Finding the actual position from the above value, which may be negative
    int startPos = (startLoc < 0) ? (-(startLoc + 1)) : startLoc;
    int prev = startPos - 1;
    // Finding the nonnegative position of the next element
    int next = (endLoc < 0) ? (-(endLoc + 1)) : endLoc;
    ArrayList<TimeRange> newBusyTimes = new ArrayList<>();

    // stores the index in the array from which this meeting begins
    int startIndex = 0;
    // stores the time at which this meeting begins
    int startTime = 0;
    // stores the index in the array at which this meeting ends
    int endIndex = 0;
    // stores the time at which this meeting ends
    int endTime = 0;

    if (prev < 0) {
      // If the meeting is to be inserted at the start of the list, startIndex = 0
      startIndex = startPos;
      startTime = meetingTime.start();
    } else {
      TimeRange prevElem = busyTimes.get(prev);
      /*
       * Otherwise find out whether this meeting overlaps with the previous meeting
       * If it does, coalesce it with the previous meeting otherwise keep them
       * separate
       */
      startIndex = prevElem.overlaps(meetingTime) ? prev : startPos;
      startTime = prevElem.overlaps(meetingTime) ? prevElem.start() : meetingTime.start();
    }

    if (next >= busyTimes.size()) {
      // If this element is to be inserted at the end of the list, endIndex = last element of list
      endIndex = next - 1;
      endTime = meetingTime.end();
    } else {
      /*
       * Otherwise find out whether this meeting overlaps with the next meeting
       * If it does, coalesce it with the next meeting otherwise keep them
       * separate
       */
      TimeRange nextElem = busyTimes.get(next);
      endIndex = nextElem.overlaps(meetingTime) ? next : next - 1;
      endTime = nextElem.overlaps(meetingTime) ? nextElem.end() : meetingTime.end();
    }

    // create a new meeting with the given start and end times
    TimeRange newBusy = TimeRange.fromStartEnd(startTime, endTime, false);

    /*
     * This meeting ends before the current first meeting starts, so add it to the
     * beginning of the list
     */
    if (endIndex < 0) {
      newBusyTimes.add(newBusy);
    }
    for (int i = 0; i < busyTimes.size(); i++) {
      // Insert the new entry in its appropriate location ordered by start times
      if (i == startIndex && endIndex >= 0) {
        newBusyTimes.add(newBusy);
      }
      // And remove all redundant entries between its start and end points
      if (i < startIndex || i > endIndex) {
        newBusyTimes.add(busyTimes.get(i));
      }
    }
    /*
     * This meeting starts after the current last meeting ends, so add it to the end of
     * the list
     */
    if (startIndex >= busyTimes.size() && endIndex >= 0) {
      newBusyTimes.add(newBusy);
    }
    return newBusyTimes;
  }

  /*
   * Given a list of busy times in non-overlapping, sorted order and a meeting duration
   * returns a list of time slots of atleast 'duration' length during which no meetings
   * are scheduled
   */
  private ArrayList<TimeRange> findFreeTimes(ArrayList<TimeRange> busyTimes, long duration) {
    ArrayList<TimeRange> freeTimes = new ArrayList<>();
    /*
     * This variable represents the start time of the current free block. It starts
     * out as the value of the start of the day and iteratively takes on the value of the
     * end of each busy block
     */
    int start = TimeRange.START_OF_DAY;
    for (int i = 0; i < busyTimes.size(); i++) {
      TimeRange curr = busyTimes.get(i);

      int thisStart = curr.start();
      int thisEnd = curr.end();
      /*
       * create a free slot from the beginning of the free period to 'thisStart',
       * the beginning of the next busy one
       */
      TimeRange newFree = TimeRange.fromStartEnd(start, thisStart, false);
      if (newFree.duration() >= duration) {
        freeTimes.add(newFree);
      }
      start = thisEnd;
    }
    /*
     * This represents the integer value of the end of the day. If possible, a free block
     * will be created from the end of the last busy block to the end of the day
     */
    int end = TimeRange.END_OF_DAY;
    TimeRange newFree = TimeRange.fromStartEnd(start, end, true);
    if (newFree.duration() >= duration) {
      freeTimes.add(newFree);
    }
    return freeTimes;
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    /*
     * Stores a list of non-overlapping time periods when at least one required
     * meeting attendee is busy.
     */
    ArrayList<TimeRange> reqBusy = new ArrayList<>();

    /*
     * Stores a list of non-overlapping time periods when at least one required or optional
     * meeting attendee is busy.
     */
    ArrayList<TimeRange> optBusy = new ArrayList<>();
    HashSet<String> attendees = new HashSet<>(request.getAttendees());
    HashSet<String> optAttendees = new HashSet<>(request.getOptionalAttendees());

    Iterator<Event> iterator = events.iterator();
    while (iterator.hasNext()) {
      Event meeting = iterator.next();
      Set<String> meetingAttendees = meeting.getAttendees();
      if (!Sets.intersection(attendees, meetingAttendees).isEmpty()) {
        // If this meeting involves required attendees, add it to both busy lists
        reqBusy = addToBusy(reqBusy, meeting);
        optBusy = addToBusy(optBusy, meeting);
      } else {
        if (!Sets.intersection(optAttendees, meetingAttendees).isEmpty()) {
          // If this meeting involves only optional attendees, add it to the optional busy list
          optBusy = addToBusy(optBusy, meeting);
        }
      }
    }
    // Try to accomodate optional attendees
    ArrayList<TimeRange> optFree = findFreeTimes(optBusy, request.getDuration());
    if (optFree.size() == 0 && attendees.size() != 0) {
      // If that fails and there are some required attendees, find time slots that accomodate them
      return findFreeTimes(reqBusy, request.getDuration());
    }
    return optFree;
  }
}
