
package com.tikal.fuseday.checkinattack;

import static java.util.stream.Collectors.toList;
import io.vertx.java.redis.RedisClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

//echo '{"userId":"e433d0f4-5074-4fb5-9b7a-c245b7f8ed6e", "latitude":32.115402, "longitude":34.843444}' | curl -i  -d @- http://localhost:8080
public class CheckinAttackVerticle extends Verticle {
	private Logger log;

	
	private JsonObject config;
	
	private final Random random = new Random();	
	
	private final String checkinTemplate = "{\"userId\":\"%s\", \"latitude\":%f, \"longitude\":%f}";
	
	private final Map<String, List<Long>> attackers = new HashMap<>(); 
	
	private final Map<String, Integer> checkinsCounters = new HashMap<>();
	
	private  RedisClient redisClient; 

	@Override
	public void start() {
		config = container.config();
		log = container.logger();	
		
		
		redisClient = new RedisClient(vertx.eventBus(), "io.vertx.mod-redis");
		
		redisClient.deployModule(container, config.getString("redis-host"), 6379, "UTF-8", false, null, 1, 1, new AsyncResultHandler<String>() {
            @Override
            public void handle(final AsyncResult<String> event) {
                if (event.failed()) {
                    log.error(event.cause().getMessage());
                } else {
                	log.info("connected to Redis");
                }
            }
        });		
		
		config.getArray("attackers").forEach(this::registerAttacker);				
		registerCheckinServer();		
	}

	
	private void registerAttacker(final Object o) {
		final List<Long> timers = new LinkedList<>();
		final JsonObject attacker = (JsonObject)o;
		for (int i = 0; i < attacker.getInteger("instances"); i++) {
			final HttpClient client = vertx.createHttpClient().setHost(attacker.getString("host")).setPort(attacker.getInteger("port"));
			timers.add(vertx.setPeriodic(attacker.getLong("delay"), t->attack(client)));
		} 
		final String key = attacker.getString("host")+":"+attacker.getInteger("port");
		attackers.put(key, timers);
		log.info("Register attacker "+key+" with timers "+timers);
	}	
	
	private void attack(final HttpClient client) {
		log.info("Attacking...");
		client.post("/checkin", r->log.info("Got a response: " + r.statusCode()))
		.putHeader("Content-Type", "application/json")
		.end(String.format(checkinTemplate,UUID.randomUUID(),-180*random.nextFloat(),180*random.nextFloat()));
	}
	
	
	

	private void registerCheckinServer() {
		final Integer port = config.getInteger("port");
		if(port==null)
			return;
		
		final HttpServer server = vertx.createHttpServer();
		final RouteMatcher routeMatcher = new RouteMatcher();

		routeMatcher.get("/checkins",req->req.response().end(new Buffer(new JsonArray(getCheckinsCounters()).toString())));
		routeMatcher.post("/checkin",(r -> r.bodyHandler(this::handleCheckin).response().end()));
		server.requestHandler(routeMatcher).listen(port, config.getString("host"));
		
		log.info("Startetd listening on port..."+port);
	}
	
	
	private List<List<Integer>> getCheckinsCounters(){
		return checkinsCounters.entrySet().stream().map(es->getMapEntry(es.getKey(),es.getValue())).collect(toList());
	}
	
	
	private List<Integer> getMapEntry(final String key, final int counter){
		final String[] split = key.split(",");
		return Arrays.asList(new Integer[]{Integer.valueOf(split[0]),Integer.valueOf(split[1]),counter});
	}
	

	

	private void handleCheckin(final Buffer body) {
		log.info("Hadling checking : "+body+"...");
		final JsonObject checkin = new JsonObject(body.getString(0, body.length()));
		final int lat = getGeoCord(checkin.getNumber("latitude"), 180);
		final int lon = getGeoCord(checkin.getNumber("longitude"), 180);
		if( isUnlegal(lon) || isUnlegal(lat))
			return;
		final String key = lat+","+lon;
		Integer counter = checkinsCounters.get(key);
		if(counter==null)
			checkinsCounters.put(key, 1);
		else
			checkinsCounters.put(key, ++counter);
		log.info("Finished hadling checkin : "+checkin);
		
		final String userId = checkin.getString("userId");
		redisClient.lpush(userId,key);

	}
	
	private boolean isUnlegal(final int number){
		return number == 404;
	}


	private int getGeoCord(final Number number, final int limit) {
		if(number.floatValue() > limit || number.floatValue() < limit*-1){
			return 404;
		}
		return number.intValue();
	}

	
}
