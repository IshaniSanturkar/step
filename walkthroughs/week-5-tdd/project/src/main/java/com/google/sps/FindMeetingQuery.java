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
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class FindMeetingQuery {

  /*
   * Given a list of busy times in non-overlapping, sorted order and a meeting duration
   * returns a list of time slots of atleast 'duration' length during which no meetings
   * are scheduled
   */
  private ArrayList<TimeRange> findFreeTimes(
      ArrayList<TimeRange> busyTimes, long duration, boolean ignoreOpt) {
    ArrayList<TimeRange> freeTimes = new ArrayList<>();
    /*
     * This variable represents the start time of the current free block. It starts
     * out as the value of the start of the day and iteratively takes on the value of the
     * end of each busy block
     */
    int start = TimeRange.START_OF_DAY;
    for (int i = 0; i < busyTimes.size(); i++) {
      TimeRange curr = busyTimes.get(i);
      if (ignoreOpt && !curr.isReq()) {
        continue;
      }

      int thisStart = curr.start();
      int thisEnd = curr.end();
      /*
       * create a free slot from the beginning of the free period to 'thisStart',
       * the beginning of the next busy one
       */
      TimeRange newFree = TimeRange.fromStartEnd(start, thisStart, false);
      if (newFree.duration() >= duration && (ignoreOpt || newFree.getOptBusy().size() == 0)) {
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

  private int coalesceOptOpt(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {
    if (first.contains(second)) {
      second.addOptBusy(first.getOptBusy());
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(first.getOptBusy());
      TimeRange newThird = TimeRange.fromStartEnd(second.end(), first.end(), false);
      newThird.addOptBusy(first.getOptBusy());
      busyTimes.set(index, newFirst);
      busyTimes.add(index + 2, newThird);
      return index + 1;
    } else if(second.contains(first)) {
      first.addOptBusy(second.getOptBusy());
      TimeRange newThird = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newThird.addOptBusy(second.getOptBusy());
      TimeRange newFirst = TimeRange.fromStartEnd(second.start(), first.start(), false);
      newFirst.addOptBusy(second.getOptBusy());
      busyTimes.set(index + 1, newThird);
      busyTimes.add(index, newFirst);
      return index + 1;
    } else {
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(first.getOptBusy());
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(second.getOptBusy());
      TimeRange newMiddle = TimeRange.fromStartEnd(second.start(), first.end(), false);
      newMiddle.addOptBusy(first.getOptBusy());
      newMiddle.addOptBusy(second.getOptBusy());
      busyTimes.set(index, newFirst);
      busyTimes.set(index + 1, newSecond);
      busyTimes.add(index + 1, newMiddle);
      return index + 1;
    }
  }

  private int coalesceOptReq(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {
    if (second.contains(first)) {
      busyTimes.remove(index);
      return index - 1;
    } else if (first.contains(second)) {
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(first.getOptBusy());
      TimeRange newSecond = TimeRange.fromStartEnd(second.end(), first.end(), false);
      newSecond.addOptBusy(first.getOptBusy());
      busyTimes.set(index, newFirst);
      busyTimes.add(index + 2, newSecond);
      return index + 1;
    } else {
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(first.getOptBusy());
      busyTimes.set(index, newFirst);
      return index;
    }
  }

  private int coalesceReqOpt(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {
    if (first.contains(second)) {
      busyTimes.remove(index + 1);
      return index - 1;
    } else if (second.contains(first)) {
      TimeRange newFirst = TimeRange.fromStartEnd(second.start(), first.start(), false);
      newFirst.addOptBusy(second.getOptBusy());
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(second.getOptBusy());
      busyTimes.set(index + 1, newSecond);
      busyTimes.add(index, newFirst);
      return index + 1;
    } else {
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(second.getOptBusy());
      busyTimes.set(index + 1, newSecond);
      return index;
    }
  }

  private int coalesceReqReq(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {
    busyTimes.remove(index);
    if (first.contains(second)) {
      busyTimes.set(index, first);
    } else if (second.contains(first)) {
      busyTimes.set(index, second);
    } else {
      busyTimes.set(index, TimeRange.fromStartEnd(first.start(), second.end(), false));
    }
    return index - 1;
  }

  private void coalesceOverlap(ArrayList<TimeRange> busyTimes) {
    for (int i = 0; i < busyTimes.size() - 1; i++) {
      TimeRange curr = busyTimes.get(i);
      TimeRange next = busyTimes.get(i + 1);
      if (curr.overlaps(next)) {
        if (curr.isReq() && next.isReq()) {
          i = coalesceReqReq(curr, next, busyTimes, i);
        } else if (curr.isReq()) {
          i = coalesceReqOpt(curr, next, busyTimes, i);
        } else if (next.isReq()) {
          i = coalesceOptReq(curr, next, busyTimes, i);
        } else {
          i = coalesceOptOpt(curr, next, busyTimes, i);
        }
      }
    }
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    ArrayList<TimeRange> busy = new ArrayList<>();
    ArrayList<TimeRange> optBusy = new ArrayList<>();
    HashSet<String> attendees = new HashSet<>(request.getAttendees());
    HashSet<String> optAttendees = new HashSet<>(request.getOptionalAttendees());
    Iterator<Event> iterator = events.iterator();
    while (iterator.hasNext()) {
      Event meeting = iterator.next();
      Set<String> meetingAttendees = meeting.getAttendees();
      if (!Sets.intersection(attendees, meetingAttendees).isEmpty()) {
        TimeRange meetingTime = meeting.getWhen();
        busy.add(meetingTime);
      } else {
        SetView<String> optInMeeting = Sets.intersection(optAttendees, meetingAttendees);
        if (optInMeeting.size() != 0) {
          TimeRange meetingTime = meeting.getWhen();
          meetingTime.addOptBusy(optInMeeting);
          busy.add(meetingTime);
        }
      }
    }
    Collections.sort(busy, TimeRange.ORDER_BY_START);
    coalesceOverlap(busy);
    ArrayList<TimeRange> freeOpt = findFreeTimes(busy, request.getDuration(), false);
    if (freeOpt.size() == 0 && attendees.size() != 0) {
      return findFreeTimes(busy, request.getDuration(), true);
    }
    return freeOpt;
  }
}
