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
   * Given a list of busy times in non-overlapping, sorted order and a meeting duration,
   * returns a list of time slots of atleast 'duration' length during which no meetings
   * are scheduled. If ignoreOpt is true, timeslots constrained only by optional attendees
   * are ignored.
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
      // If we can ignore times where optional attendees are busy and this is one of them, continue
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

  /*
   * Definitions:
   * Optional block - one that can be attended by all required attendees but not by some
   * optional attendees (|---optional---|)
   * Required block - one that cannot be attended by all required attendees (|__required___|)
   */

  /*
   * Coalesces two overlapping optional blocks (constrained only by optional attendees)
   * at index 'index' and 'index + 1' in busyTimes, returning the index before the
   * next index to proceed with coalescing from
   */
  private int coalesceOptOpt(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {
    /*
     * Lists of those optional attendees of the new meeting who are busy during the first and second
     * time ranges
     */
    HashSet<String> firstOptBusy = first.getOptBusy();
    HashSet<String> secondOptBusy = second.getOptBusy();

    if (first.contains(second)) {
      /*
       * Before coalesce: |-------------first--------------|
       *                          |-----second-----|
       *
       * After coalesce:  |--nF--||-----second-----||--nT--|
       */
      second.addOptBusy(firstOptBusy);
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      TimeRange newThird = TimeRange.fromStartEnd(second.end(), first.end(), false);
      newThird.addOptBusy(firstOptBusy);
      busyTimes.set(index, newFirst);
      busyTimes.add(index + 2, newThird);
      return index + 1;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |-------------second--------------|
       *                  |-----first-----|
       *
       * Note that since first is before second in the sorted list, it must start before
       * or at the same time as second
       *
       * After coalesce:  |-----first-----||-------nS-------|
       */
      first.addOptBusy(secondOptBusy);
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(secondOptBusy);
      busyTimes.set(index + 1, newSecond);
      return index;
    } else {
      /*
       * Before coalesce: |-------------first--------------|
       *                          |---------------second-------------|
       *
       * After coalesce:  |--nF--||------------nS----------||---nT---|
       */
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      TimeRange newSecond = TimeRange.fromStartEnd(second.start(), first.end(), false);
      newSecond.addOptBusy(firstOptBusy);
      newSecond.addOptBusy(secondOptBusy);
      TimeRange newThird = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newThird.addOptBusy(secondOptBusy);
      busyTimes.set(index, newFirst);
      busyTimes.set(index + 1, newThird);
      busyTimes.add(index + 1, newSecond);
      return index + 1;
    }
  }

  /*
   * Coalesces two overlapping blocks, the first optional and the second required
   * at index 'index' and 'index + 1' in busyTimes, returning the index before the
   * next index to proceed with coalescing from
   */
  private int coalesceOptReq(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {
    /*
     * List of those optional attendees of the new meeting who are busy during the first
     * time range
     */
    HashSet<String> firstOptBusy = first.getOptBusy();
    if (first.contains(second)) {
      /*
       * Before coalesce: |-------------first-------------|
       *                          |_____second____|
       *
       * After coalesce:  |--nF--||_____second____||--nT--|
       */
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      TimeRange newThird = TimeRange.fromStartEnd(second.end(), first.end(), false);
      newThird.addOptBusy(firstOptBusy);
      busyTimes.set(index, newFirst);
      busyTimes.add(index + 2, newThird);
      return index + 1;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |___________second___________|
       *                  |----first-----|
       *
       * After coalesce:  |___________second___________|
       */
      busyTimes.remove(index);
      return index - 1;
    } else {
      /*
       * Before coalesce: |----first-----|
       *                          |___________second___________|
       *
       * After coalesce:  |--nF--||___________second___________|
       */
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      busyTimes.set(index, newFirst);
      return index;
    }
  }

  /*
   * Coalesces two overlapping blocks, the first required and the second optional
   * at index 'index' and 'index + 1' in busyTimes, returning the index before the
   * next index to proceed with coalescing from
   */
  private int coalesceReqOpt(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {

    /*
     * List of those optional attendees of the new meeting who are busy during the second
     * time range
     */
    HashSet<String> secondOptBusy = second.getOptBusy();
    if (first.contains(second)) {
      /*
       * Before coalesce: |___________first___________|
       *                          |----second-----|
       *
       * After coalesce:  |___________first___________|
       */
      busyTimes.remove(index + 1);
      return index - 1;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |-------------second-------------|
       *                  |_____first____|
       *
       * After coalesce:  |_____first____||-------nS-------|
       */
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(secondOptBusy);
      busyTimes.set(index + 1, newSecond);
      return index;
    } else {
      /*
       * Before coalesce: |____first____|
       *                          |------second-----|
       *
       * After coalesce:  |_____first___||----nS----|
       */
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(secondOptBusy);
      busyTimes.set(index + 1, newSecond);
      return index;
    }
  }

  /*
   * Coalesces two overlapping required blocks at index 'index' and 'index + 1' in busyTimes,
   * returning the index before the next index to proceed with coalescing from
   */
  private int coalesceReqReq(
      TimeRange first, TimeRange second, ArrayList<TimeRange> busyTimes, int index) {

    if (first.contains(second)) {
      /*
       * Before coalesce: |___________first_____________|
       *                         |___second___|
       *
       * After coalesce:  |___________first_____________|
       */
      busyTimes.remove(index + 1);
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |___________second_____________|
       *                  |___first___|
       *
       * After coalesce:  |___________first_____________|
       */
      busyTimes.remove(index);
    } else {
      /*
       * Before coalesce: |___________first_____________|
       *                                 |_______second_________|
       *
       * After coalesce:  |_____________nC______________________|
       */
      TimeRange newCombined = TimeRange.fromStartEnd(first.start(), second.end(), false);
      busyTimes.remove(index);
      busyTimes.set(index, newCombined);
    }
    return index - 1;
  }

  /*
   * Iterates through a list of time ranges sorted by start times (and for equal start times,
   * end times). Each time range must be annotated as required or optional busy. Modifies the
   * list into a list of non-overlapping time ranges annotated either as required busy or as
   * optional busy with all optional attendees who cannot make it during this time listed.
   */
  private void coalesceOverlap(ArrayList<TimeRange> busyTimes) {
    int i = 0;
    while (i < busyTimes.size() - 1) {
      TimeRange curr = busyTimes.get(i);
      TimeRange next = busyTimes.get(i + 1);
      /*
       * Coalesce the two blocks into one if they overlap and update the loop index
       * to be the one before the next block that needs to be coalesced
       */
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
      i++;
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
