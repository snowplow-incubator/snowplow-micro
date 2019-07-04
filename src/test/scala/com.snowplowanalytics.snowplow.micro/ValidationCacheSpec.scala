/*
 * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.micro

import org.specs2.mutable.Specification

class ValidationCacheSpec extends Specification {

  val emptyFilterGood = FiltersGood(None, None, None, None)
  val emptyFilterBad = FiltersBad(None, None, None)

  // create good event 1
  // create good event 2
  // create good event 3

  // create bad event 1
  // create bad event 2


  "getSummary" >> {
    "should return the correct number of bad and good events" >> {
      ValidationCache.getSummary().good must_== ValidationCache.filterGood(emptyFilterGood).size
      ValidationCache.getSummary().bad must_== ValidationCache.filterBad(emptyFilterBad).size
      ValidationCache.getSummary().total must_== ValidationCache.filterGood(emptyFilterGood).size +
        ValidationCache.filterBad(emptyFilterBad).size

      // add 2 good and 1 bad

      ValidationCache.getSummary().good must_== ValidationCache.filterGood(emptyFilterGood).size
      ValidationCache.getSummary().bad must_== ValidationCache.filterBad(emptyFilterBad).size
      ValidationCache.getSummary().total must_== ValidationCache.filterGood(emptyFilterGood).size +
        ValidationCache.filterBad(emptyFilterBad).size
    }
  }

  "addToGood" >> {
    "should succesfully add one good event to an empty cache" >> {
      ValidationCache.reset()
      // add event 1
      // check that cache contains only it
      1 must_== 1
    }

    // might have to deactivate parallelism
    "should succesfully add several good events to an empty cache" >> {
      ValidationCache.reset()
      // add event 1 and 2
      // check that cache contains only these 2
      1 must_== 1
    }
    
    "should succesfully add good events to a non empty cache" >> {
      ValidationCache.reset()
      // add event 1
      // add event 2 and 3
      // check that cache contains them 
      1 must_== 1
    }
  }
  
  "addToBad" >> {
    "should succesfully add one bad event to an empty cache" >> {
      ValidationCache.reset()
      // add event 1
      // check that cache contains only it
      1 must_== 1
    }

    // might have to deactivate parallelism
    "should succesfully add several bad events to an empty cache" >> {
      ValidationCache.reset()
      // add event 1 and 2
      // check that cache contains only these 2
      1 must_== 1
    }
    
    "should succesfully add bad events to a non empty cache" >> {
      ValidationCache.reset()
      // add event 1
      // add event 2 and 3
      // check that cache contains them 
      1 must_== 1
    }
  }

  "reset" >> {
    "should remove all the good and bad events from the cache" >> {
      // add a good event
      // add a bad event
      // check not empty
      ValidationCache.reset()
      ValidationCache.getSummary().good must_== 0
      ValidationCache.getSummary().bad must_== 0
    }
  }

  "filterGood" >> {
    "should return only the good events for which the filter matches" >> {
      // put 3 events in cache
      // filter that returns all of them
      // filter that returns 2 of them
      // filter that returns 1 of them
      // filter that returns none of them
      1 must_== 1
    }

    "should return the most recent events if limit is set" >> {
      1 must_== 1
    }
  }

  "keepGoodEvent" >> {
    "should correctly return true or false if the filter contains only an event_type and it matches" >> {
      // true if same event type
      // false if not
      1 must_== 1
    }
    
    "should correctly return true or false if the filter contains only a schema and it matches" >> {
      // ue event with same schema
      // ue event with different schema
      // non ue event
      1 must_== 1
    }
    
    "should correctly return true or false if the filter contains only contexts and it matches" >> {
      1 must_== 1
    }

    "should correctly return true or false if several filters are set at the same time" >> {
      1 must_== 1
    }
  }

  "containsAllContext" >> {
    "should return true if the event contains exactly the same contexts" >> {
      1 must_== 1
    }
    
    "should return true if the event contains the same contexts and more" >> {
      1 must_== 1
    }
    
    "should return true if list of contexts in the filter is empty" >> {
      1 must_== 1
    }
  }
  
  "filterBad" >> {
    "should return only the bad events for which the filter matches" >> {
      // put 2 events in cache
      // filter that returns both of them
      // filter that returns 1 of them
      // filter that returns none of them
      1 must_== 1
    }
  }
  
  "keepBadEvent" >> {
    "should correctly filter out based on the vendor" >> {
      1 must_== 1
    }
    
    "should correctly filter out based on the version" >> {
      1 must_== 1
    }
    
    "should correctly filter out based on the vendor and the version at the same time" >> {
      1 must_== 1
    }
  }
}
