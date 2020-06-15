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
    int startIndex = Collections.binarySearch(busyTimes, meetingTime, TimeRange.ORDER_BY_START);
    // The position where the end time of the meeting fits into the sorted list
    int endIndex = Collections.binarySearch(busyTimes, meetingTime, TimeRange.ORDER_BY_END);
    // Finding the actual position from the above value, which may be negative
    int startPos = (startIndex < 0) ? (-(startIndex + 1)) : startIndex;
    int prev = startPos - 1;
    // Finding the nonnegative position of the next element
    int next = (endIndex < 0) ? (-(endIndex + 1)) : endIndex;
    ArrayList<TimeRange> newBusyTimes = new ArrayList<>();

    // stores the index in the array from which this meeting begins
    int startPoint = 0;
    // stores the time at which this meeting begins
    int startTime = 0;
    // stores the index in the array at which this meeting ends
    int endPoint = 0;
    // stores the time at which this meeting ends
    int endTime = 0;

    if (prev < 0) {
      // If the meeting is to be inserted at the start of the list, startPoint = 0
      startPoint = startPos;
      startTime = meetingTime.start();
    } else {
      TimeRange prevElem = busyTimes.get(prev);
      /*
       * Otherwise find out whether this meeting overlaps with the previous meeting
       * If it does, coalesce it with the previous meeting otherwise keep them
       * separate
       */
      startPoint = prevElem.overlaps(meetingTime) ? prev : startPos;
      startTime = prevElem.overlaps(meetingTime) ? prevElem.start() : meetingTime.start();
    }

    if (next >= busyTimes.size()) {
      // If this element is to be inserted at the end of the list, endPoint = last element of list
      endPoint = next - 1;
      endTime = meetingTime.end();
    } else {
      /*
       * Otherwise find out whether this meeting overlaps with the next meeting
       * If it does, coalesce it with the next meeting otherwise keep them
       * separate
       */
      TimeRange nextElem = busyTimes.get(next);
      endPoint = nextElem.overlaps(meetingTime) ? next : next - 1;
      endTime = nextElem.overlaps(meetingTime) ? nextElem.end() : meetingTime.end();
    }

    // create a new meeting with the given start and end times
    TimeRange newBusy = TimeRange.fromStartEnd(startTime, endTime, false);

    /*
     * This meeting ends before the current first meeting starts, so add it to the
     * beginning of the list
     */
    if (endPoint < 0) {
      newBusyTimes.add(newBusy);
    }
    for (int i = 0; i < busyTimes.size(); i++) {
      if (i == startPoint && endPoint >= 0 && startPoint < busyTimes.size()) {
        // Replace all the entries between startPoint and endPoint with the coalesced entry
        newBusyTimes.add(newBusy);
        i = endPoint;
      } else {
        newBusyTimes.add(busyTimes.get(i));
      }
    }
    /*
     * This meeting starts after the current last meeting ends, so add it to the end of
     * the list
     */
    if (startPoint >= busyTimes.size() && endPoint >= 0) {
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
    Collections.sort(busyTimes, TimeRange.ORDER_BY_START);
    ArrayList<TimeRange> freeTimes = new ArrayList<>();
    int start = TimeRange.START_OF_DAY;
    int end = TimeRange.END_OF_DAY;
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
    // Create a free slot from the end of the last meeting to the end of the day if possible
    TimeRange newFree = TimeRange.fromStartEnd(start, end, true);
    if (newFree.duration() >= duration) {
      freeTimes.add(newFree);
    }
    return freeTimes;
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    ArrayList<TimeRange> busy = new ArrayList<>();
    HashSet<String> attendees = new HashSet<>(request.getAttendees());
    Iterator<Event> iterator = events.iterator();
    while (iterator.hasNext()) {
      Event meeting = iterator.next();
      Set<String> meetingAttendees = meeting.getAttendees();
      if (Sets.intersection(attendees, meetingAttendees).isEmpty()) {
        /*
         * If none of the meeting attendees need to be there for the new meeting
         * ignore it
         */
        continue;
      } else {
        busy = addToBusy(busy, meeting);
      }
    }
    return findFreeTimes(busy, request.getDuration());
  }
}
