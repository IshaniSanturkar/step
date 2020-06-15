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
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public final class FindMeetingQuery {

  private ArrayList<TimeRange> addToBusy(ArrayList<TimeRange> busyTimes, Event meeting) {
    TimeRange meetingTime = meeting.getWhen();
    int startIndex = Collections.binarySearch(busyTimes, meetingTime, TimeRange.ORDER_BY_START);
    int endIndex = Collections.binarySearch(busyTimes, meetingTime, TimeRange.ORDER_BY_START);
    int currStart = (startIndex < 0) ? (-(startIndex + 1)) : startIndex;
    int prev = currStart - 1;
    int next = (endIndex < 0) ? (-(endIndex + 1)) : endIndex;
    ArrayList<TimeRange> newBusyTimes = new ArrayList<>();

    int startPoint = 0;
    int startTime = 0;
    int endPoint = 0;
    int endTime = 0;

    if (prev < 0) {
      startPoint = currStart;
      startTime = meetingTime.start();
    } else {
      TimeRange prevElem = busyTimes.get(prev);
      startPoint = prevElem.overlaps(meetingTime) ? prev : currStart;
      startTime = prevElem.overlaps(meetingTime) ? prevElem.start() : meetingTime.start();
    }

    if (next >= busyTimes.size()) {
      endPoint = next - 1;
      endTime = meetingTime.end();
    } else {
      TimeRange nextElem = busyTimes.get(next);
      endPoint = nextElem.overlaps(meetingTime) ? next : next - 1;
      endTime = nextElem.overlaps(meetingTime) ? nextElem.end() : meetingTime.end();
    }
    TimeRange newBusy = TimeRange.fromStartEnd(startTime, endTime, false);
    if(endPoint < 0) {
      newBusyTimes.add(newBusy);
    }
    for(int i = 0; i < busyTimes.size(); i++) {
      if(i == startPoint && endPoint >= 0 && startPoint < busyTimes.size()) {
        System.out.println(startPoint + " " + endPoint);
        newBusyTimes.add(newBusy);
        i = endPoint;
      } else {
        newBusyTimes.add(busyTimes.get(i));
      }
      // if (i >= startPoint && i <= endPoint) {
      //   continue;
      // }
      // if(i == startPoint && endPoint >= 0 && startPoint < busyTimes.size()) {
      //   newBusyTimes.add(newBusy);
      // } else {
      //   newBusyTimes.add(busyTimes.get(i));
      // }
    }
    if (startPoint >= busyTimes.size() || endPoint <= 0) {
      newBusyTimes.add(newBusy);
    }
    return newBusyTimes;
  }

  private ArrayList<TimeRange> findFreeTimes(ArrayList<TimeRange> busyTimes, long duration) {
    System.out.println(busyTimes);
    Collections.sort(busyTimes, TimeRange.ORDER_BY_START);
    ArrayList<TimeRange> freeTimes = new ArrayList<>();
    int start = TimeRange.START_OF_DAY;
    int end = TimeRange.END_OF_DAY;
    for (int i = 0; i < busyTimes.size(); i++) {
      TimeRange curr = busyTimes.get(i);

      int thisStart = curr.start();
      int thisEnd = curr.end();
      if (start != thisStart) {
        TimeRange newFree = TimeRange.fromStartEnd(start, thisStart, false);
        // if(newFree.duration() < duration) {
        //   continue;
        // }
        freeTimes.add(newFree);
      }
      start = thisEnd;
    }
    if(start != end) {
      TimeRange newFree = TimeRange.fromStartEnd(start, end, true);
      freeTimes.add(newFree);
      // if(newFree.duration() >= duration) {
      //   freeTimes.add(newFree);
      // }
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
        continue;
      } else {
        busy = addToBusy(busy, meeting);
      }
    }
    return findFreeTimes(busy, request.getDuration());
  }
}
