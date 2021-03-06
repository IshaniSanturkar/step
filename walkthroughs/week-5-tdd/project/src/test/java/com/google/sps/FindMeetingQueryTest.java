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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class FindMeetingQueryTest {
  private static final Collection<Event> NO_EVENTS = Collections.emptySet();
  private static final Collection<String> NO_ATTENDEES = Collections.emptySet();

  // Some people that we can use in our tests.
  private static final String PERSON_A = "Person A";
  private static final String PERSON_B = "Person B";
  private static final String PERSON_C = "Person C";
  private static final String PERSON_D = "Person D";
  private static final String PERSON_E = "Person E";

  // All dates are the first day of the year 2020.
  private static final int TIME_0800AM = TimeRange.getTimeInMinutes(8, 0);
  private static final int TIME_0830AM = TimeRange.getTimeInMinutes(8, 30);
  private static final int TIME_0900AM = TimeRange.getTimeInMinutes(9, 0);
  private static final int TIME_0930AM = TimeRange.getTimeInMinutes(9, 30);
  private static final int TIME_1000AM = TimeRange.getTimeInMinutes(10, 0);
  private static final int TIME_1030AM = TimeRange.getTimeInMinutes(10, 30);
  private static final int TIME_1100AM = TimeRange.getTimeInMinutes(11, 00);

  private static final int DURATION_15_MINUTES = 15;
  private static final int DURATION_30_MINUTES = 30;
  private static final int DURATION_60_MINUTES = 60;
  private static final int DURATION_90_MINUTES = 90;
  private static final int DURATION_1_HOUR = 60;
  private static final int DURATION_2_HOUR = 120;

  private FindMeetingQuery query;

  @Before
  public void setUp() {
    query = new FindMeetingQuery();
  }

  @Test
  public void optionsForNoAttendees() {
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_1_HOUR);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noOptionsForTooLongOfARequest() {
    // The duration should be longer than a day. This means there should be no options.
    int duration = TimeRange.WHOLE_DAY.duration() + 1;
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), duration);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void eventSplitsRestriction() {
    // The event should split the day into two options (before and after the event).
    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsidered() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeeNotRequired() {
    // Have the mandatory attendees have two different events. The optional attendee
    // is busy all day, so we should just see 3 options because each mandatory person has
    // split the restricted times.
    //
    // Mandatory Events  :       |--A--|     |--B--|
    // Optional Events   : |--------------C--------------|
    // Day               : |-----------------------------|
    // Options           : |--1--|     |--2--|     |--3--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 3",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeeConsidered() {
    // Have the mandatory attendees have two different events. They split the day into
    // three free time slots. The optional attendee is busy in one of them, so only the
    // other two should be returned.
    //
    // Mandatory Events  :       |--A--|     |--B--|
    // Optional Events   :             |--C--|
    // Day               : |-----------------------------|
    // Options           : |--1--|                 |--3--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void overlappingEvents() {
    // Have an event for each person, but have their events overlap. We should only see two options.
    //
    // Events  :       |--A--|
    //                     |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void nestedEvents() {
    // Have an event for each person, but have one person's event fully contain another's event. We
    // should see two options.
    //
    // Events  :       |----A----|
    //                   |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_90_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void doubleBookedPeople() {
    // Have one person, but have them registered to attend two events at the same time.
    //
    // Events  :       |----A----|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void justEnoughRoom() {
    // Have one person, but make it so that there is just enough room at one point in the day to
    // have the meeting.
    //
    // Events  : |--A--|     |----A----|
    // Day     : |---------------------|
    // Options :       |-----|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void notEnoughRoomWithOptional() {
    // Have one mandatory person, but make it so that there is just enough room at one point in
    // the day to have the meeting. The optional attendee is busy during part of this time slot,
    // and since the rest of it is too small to hold the meeting, the optional attendee's
    // constraint is ignored
    //
    // Events           : |--A--|       |----A----|
    // Optional Events  :       |-C-|
    // Day              : |-----------------------|
    // Options          :       |-------|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_15_MINUTES),
                Arrays.asList(PERSON_C)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void ignoresPeopleNotAttending() {
    // Add an event, but make the only attendee someone different from the person looking to book
    // a meeting. This event should not affect the booking.
    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)));
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noConflicts() {
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void notEnoughRoom() {
    // Have one person, but make it so that there is not enough room at any point in the day to
    // have the meeting.
    //
    // Events  : |--A-----| |-----A----|
    // Day     : |---------------------|
    // Options :

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeesOnly() {
    // All attendees are optional. Have each person have different events. We should see four
    // options
    // because each person has split the restricted times.
    //
    // (Optional) Events  :       |--A--|     |--B--|
    // Day                : |-----------------------------|
    // Options            : |--1--|     |--2--|     |--3--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeesBusy() {
    // All attendees are optional. Have each person have different events so that there
    // are no free times that work for everyone. All the pieces should be returned since
    // they individually work for one optional attendee.
    //
    // (Optional) Events  : |--B--||--A--||-B-||-A-||--B--|
    // Day                : |-----------------------------|
    // Options            :

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 5",
                TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void maxOptionalAttendees() {
    // Since no time works for all optional and required attendees, return the times
    // that work for as many optional and all required attendees
    //
    // Events           :       |--A--|     |--A--|
    // Optional Events  : |--B--|                  |--B--|
    // Optional Events  :             |--C--|
    // Optional Events  :             |--D--|
    // Day              : |------------------------------|
    // Options          : |--1--|                  |--2--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_C, PERSON_D)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_1000AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, false),
                Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void maxOptionalAttendeesMult() {
    // Since no time works for all optional and required attendees, return the times
    // that work for as many optional and all required attendees
    //
    // Events           :       |--A--|     |--A--|
    // Optional Events  : |--B--|                  |--B--|
    // Optional Events  :             |--C--|      |--C--|
    // Optional Events  : |--D--|     |--D--|
    // Day              : |------------------------------|
    // Options          : |--1--|     |--2--|      |--3--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
                Arrays.asList(PERSON_B, PERSON_D)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_C, PERSON_D)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_1000AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, false),
                Arrays.asList(PERSON_B, PERSON_C)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAllBusy() {
    // Since no time works for all optional and required attendees, return the times
    // that work for as many optional (0) and all required attendees
    //
    // Events           :       |--A--|     |--A--|
    // Optional Events  : |--B--|     |--B--|      |--B--|
    // Optional Events  : |--C--|     |--C--|      |--C--|
    // Optional Events  : |--D--|     |--D--|      |--D--|
    // Day              : |------------------------------|
    // Options          : |--1--|     |--2--|      |--3--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
                Arrays.asList(PERSON_B, PERSON_C, PERSON_D)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_B, PERSON_C, PERSON_D)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_1000AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, false),
                Arrays.asList(PERSON_B, PERSON_C, PERSON_D)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(
            TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void combineBlocks() {
    // Since no time works for all optional and required attendees, return the times
    // that work for as many optional (1) and all required attendees. This tests that
    // the two chunks created from C and D's meeting are combined to create a larger
    // valid chunk
    //
    // Events           : |-----A-----|                    |-----A-----|
    // Optional Events  :              |-C-||-D-||----C----|
    // Optional Events  :                        |----D----|
    // Optional Events  :                        |----B----|
    // Day             : |---------------------------------------------|
    // Options           :              |----1---|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartEnd(TIME_1030AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_C)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_D)),
            new Event(
                "Event 5",
                TimeRange.fromStartDuration(TIME_0930AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_B, PERSON_C, PERSON_D)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_2_HOUR));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void combineBlocksButStillConsiderExisting() {
    // Since no time works for all optional and required attendees, return the times
    // that work for as many optional (3) and all required attendees. This tests that
    // an existing slot where 3 optional attendees can't make it is chosen over a
    // combined one where 4 optional attendees can't make it
    //
    // Events           : |-----A-----|                    |-----A-----|
    // Optional Events  :              |-C-||-D-||----C----|
    // Optional Events  :       |----B-----||-----E--------|
    // Optional Events  :                        |----B----|
    // Day             : |---------------------------------------------|
    // Options           :                       |----1---|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartEnd(TIME_1030AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_C)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_D)),
            new Event(
                "Event 5",
                TimeRange.fromStartDuration(TIME_0930AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_B, PERSON_C)),
            new Event(
                "Event 6",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 7",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_90_MINUTES),
                Arrays.asList(PERSON_E)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0930AM, DURATION_60_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void longEnoughBlock() {
    // Make sure that block with max optional attendees who can make it is long enough,
    // here the block that B and C are busy in is long enough so that should be returned over
    // the one where they are free which is too short
    //
    // Events           :       |--A--|            |--A--|
    // Optional Events  :              |--B--||-D-|
    // Optional Events  :              |--C--||-E-|
    // Day              : |------------------------------|
    // Options          :              |--1--|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_60_MINUTES),
                Arrays.asList(PERSON_B, PERSON_C)),
            new Event(
                "Event 3",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_D, PERSON_E)),
            new Event(
                "Event 4",
                TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0800AM, DURATION_60_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalLongestPossible() {
    // Create longest possible block of optional attendees
    //
    // Events             : |--C--|                         |--C--|
    // Optional Events    :        |-A-||-B-||-A-||-B-||-A-|
    // Day                : |-------------------------------------|
    // Options            :        |-----------1-----------|

    Collection<Event> events =
        Arrays.asList(
            new Event(
                "Event 1",
                TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
                Arrays.asList(PERSON_C)),
            new Event(
                "Event 2",
                TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 3",
                TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 4",
                TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 5",
                TimeRange.fromStartEnd(TIME_0930AM, TIME_1000AM, false),
                Arrays.asList(PERSON_B)),
            new Event(
                "Event 6",
                TimeRange.fromStartDuration(TIME_1000AM, DURATION_30_MINUTES),
                Arrays.asList(PERSON_A)),
            new Event(
                "Event 7",
                TimeRange.fromStartEnd(TIME_1030AM, TimeRange.END_OF_DAY, true),
                Arrays.asList(PERSON_C)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_C), DURATION_60_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TIME_0800AM, TIME_1030AM, false));

    Assert.assertEquals(expected, actual);
  }
}
