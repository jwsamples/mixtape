# Running the application

## Using docker (recommended)

First start an sbt shell:

```docker run -it --rm -v $PWD:/app -w /app mozilla/sbt sbt shell```

Within that shell:

```test``` runs tests and ```run /app/src/main/resources/mixtape.json /app/src/main/resources/changes.json``` runs the app.

## Natively using JDK 8

The app has only been tested on JDK 8.  It will likely work on versions up to 11.  This is an unfortunate limitation of the scala ecosystem.

Install scala build tool following the instructions here:

https://www.scala-sbt.org/1.x/docs/Setup.html

From the root source directory run:

```sbt```

Once inside the sbt shell:

```test``` runs tests and ```run <PATH TO SRC>/src/main/resources/mixtape.json <PATH TO SRC>/src/main/resources/changes.json``` runs the app.

# Structure
* ```App``` is the main class and defines the overall flow of the program.  
* ```io``` contains code for getting database and changes to and from disk.
* ```appliers``` and ```validators``` contain code for validating changes and applying them to some database.
* ```model```contains the domain model

# Discussion
* The app is not well-suited to its purpose (as a code sample).  I ended up adding a lot of unnecessary things quickly rather then focusing on doing the core thing simply and well.  It's also not super-accessible to non-Scala programmers in at least several places.  
* Test coverage is not great.  At the very least, I would have liked to have added a test that verified that everything worked when wired together.  
*
   
# Scaling 
If I needed to handle extremely large database files and large change sets, I would do two things to start:

* Use a streaming json parser to  load database file into an actual datastore, which will presumably be much better at handling queries and transformations on a massive dataaset than any sort of hand-rolled solution I might come up with.  (Assuming I structure the data in a reasonable way.)
* Use a streaming json parser to grab individual changes and push them on to a kafka topic for app to consume.     

I'd move on to make further changes (described below) from there.

## Bounding resource usage

The app already loads one change request at a time and processes these change request using a reactive, pull-based stream.  As a result, resource usage is effectively constant.  

NOTE:  While app's resource usage is likely to be constant, the datastore's usage may not be -- as it gets larger queries may become more expensive.

## Keeping latency low

While the app will not be exhaust its resources because of request volume, a long queue of unprocessed requests will start to build if it can't process them quickly enough, which in turn means that proccessing of change file will slow down.  

### Instrumentation and profiling -- improve based on real data

I would instrument the app, datastore, and whatever is running the app with a number of metrics.  Ideally these would include resource usage, throughput at various points in the pipeline, lag on request topic, latency, and error rate.

I'd use these measures to guide my optimization efforts as much as possible instead of blindly speculating about where bottlenecks might be.

I'd also use these measures to decide when I'd optimized the app sufficiently, project its likely performance under hypothetical load, and alert myself when problems crop up.

### Horizontal scaling 
The most basic technique I'd use to increase throughput would be horizontal scaling.  I'd configure a number of instances of the app to run in parallel.  (This number might change dynamically in response to queue depth.)  All the instances would consume requests from the topic feeding the app, and they'd all mutate the same datastore.

The ordering of my messages matters to an extent.  I cannot add a song to a playlist that does not yet exist, for example.  As a result, I'd partition the messages based on playlist id.  This way, all messages related to a particular playlist would be ordered, but it would still be possible for multiple instances of the app to pull requests and process them in parallel.

### Request batching and optimization

In some cases, one request invalidates or duplicates another.  For example, a remove playlist request, makes the add song request before it irrelevant.  If warranted based on request pattern and datastore load, I might batch requests in a rolling window and drop all unnecessary requests before persisting to datastore.

With some datastores, there's an especially high cost to individual one off updates.  (For example, the overhead of opening and committing a transaction.)  If supported by the datastore, I'd likely figure out a way to batch up multiple requests into a single bulk update request.

Batching would be complicated a bit because we need to do reads to validate some requests.  I discuss some workarounds for this in the "Datastore scaling" section.

### Datastore scaling

The datastore is an important bottle neck and single point of failure in this system.

I don't know what else the datastore might be used for, but taking into account only this app, the load pattern would probably be something like 40/60 reads/writes (very rough guess).  

I'd probably try to make the load (at least at ingestion) more write-heavy.

Since all of my validation constraints are just preserving referential integrity, with postgres, I could enforce these at the database level, bulk inserting a number of records, then reacting to individual failed inserts if necessary.  Other constraints could be enforced using database constraints, or by a pre-processor if they didn't involve database lookups.

In this scenario, writes would be de-coupled from reads, so I could split off some read replicas for the read workload, both increasing read performance and making locking issues less of a problem.

Another option might be to denormalize the playlist.  This sort of data would be a better fit for a document database that supports horizontal scaling, like mongo or dynamodb.  

Hydrating user ids and song ids could still be a potential problem.  If they change slowly, I might consider an in-memory caching layer for them.  (I'm intentionally not worrying about how playlists would be updated if a song or user changes here.)

A final option would be to forget traditional datastores altogether and go with a pure streaming option, powered by something like Kafka Streams.  With kstreams, I could have a log-compacted topic that represents the current state of each playlist.  I could then have a ktable to access this data to join incoming events against, along with global ktables with user and song info.  (Ultimately there would still be datastores involved under the kstreams hood.)  