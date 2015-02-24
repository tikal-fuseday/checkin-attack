***FuseDay 2015 - Checking Tack***

***Introduction***

In this fuse day we developed a simple server, which has two main
functionalities: (1) Process check-ins HTTP (POST) requests, and (2) Use
“attackers” to attack other similar servers, developed by other groups
on the Fuse day. These attackers create random Checkin requests, while
the other groups servers attacked our check-in server.

On the technology stack, we used Vert.X platform and Redis for the
back-end . On the front-end we used JavaScript code in order to present
the data on the browser, as pixels on a map. This way the end user can
see the “heat” areas with many check-ins.

We chose Vert.X as it’s a lightweight and high performance server, based
on Reactor pattern. We think its very appropriate for this use case, as
we have a short processing with high throughput capacity of check-ins,
and on the other hand we wanted to emit check-ins requests, as many as
we can, to other servers developed the other groups.

As you can see [in the GitHub
project](mailto:https://github.com/tikalk/checkin-attack) the code is
essentially one single Vrticle class, which encompass the logic and is
still very short, as all the IO logic for both incoming and outgoing
checkins is implemented efficiently in the Vert.X platform.

***Design & Implementation***

As said, our code is extremely simple and short – Its only one class
called CheckinAttackVerticle. The “start” method (invoked on Vertcile
startup) does 3 things: (a) Register the Redis module (think “driver”
for Vert.X which provides async api for Redis) , (b) Register the
Check-ins listener as HTTP Service, and (c) Register “attackers”
instances (Vert.X HTTPClient class) which emit Checkins requests.

The attack method used by the attackers, send a simple HTTP POST
requests on intervals (using VertX Timer functionality), dictated by
configuration file. The handler simply prints the reply log for
statistics.

The HTTP server listens for Checkins requests, and uses the mod-redis to
persist each and every request into the Redis DB. Redis is used to put
all check-ins on the same area, using rounded “longitude+latitude” as a
key , and “counting” for this. We used Redis “incr” increment api for
that purpose.

Last but not least, we provided REST api for the client, which
periodically polls counting for each area on the map, and could be
presented on the screen.

***Conclusion***

Working with Vert.X is somewhat different than “standard” Java
programming as its very “callback” oriented.

Using Java8 lambda expressions highly reduce and used extensively used
as callbacks, and its very appleacable on the Vert.X api due to its
“callback everywhere” orientation.

Vert.X reduces dramatically the code, while keeping it very efficient
for both client and server NIO async code.
