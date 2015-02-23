
package com.tikal.fuseday.checkinattack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

//echo '{"userId":"e433d0f4-5074-4fb5-9b7a-c245b7f8ed6e", "latitude":32.115402, "longitude":34.843444}' | curl -i  -d @- http://localhost:8080
public class CheckinAttackVerticle extends Verticle {
	private Logger log;
	
//	private final Map<String, float[]> locations = new HashMap<>();
	
//	private HttpClient client;
	
	private JsonObject config;
	
	private final Random random = new Random();	
	
	private final String checkinTemplate = "{\"userId\":\"%s\", \"latitude\":%f, \"longitude\":%f}";
	
	private final Map<String, List<Long>> attackers = new HashMap<>(); 

	@Override
	public void start() {
		config = container.config();
		log = container.logger();
		log.info("Start CheckinVerticle with config"+config);
		
		config.getArray("attackers").forEach(this::registerAttacker);
		log.info("attackers:"+attackers);
				
		vertx.createHttpServer()
		.requestHandler(r -> r.bodyHandler(this::handle)
		.response().end())
		.listen(config.getInteger("port"), config.getString("host"));
		
	}

	private void registerAttacker(final Object o) {
		final List<Long> timers = new LinkedList<>();
		final JsonObject attacker = (JsonObject)o;
		for (int i = 0; i < attacker.getInteger("instances"); i++) {
			final HttpClient client = vertx.createHttpClient().setHost(attacker.getString("host")).setPort(attacker.getInteger("port"));
			timers.add(vertx.setPeriodic(attacker.getLong("delay"), t->attack(client)));
		} 
		attackers.put(attacker.getString("host")+":"+attacker.getInteger("port"), timers);
	}
	
	
	private void attack(final HttpClient client) {
		log.info("Attacking...");
		client.put("/checkin", r->log.info("Got a response: " + r.statusCode()))
		.putHeader("Content-Type", "application/json")
		.end(String.format(checkinTemplate,UUID.randomUUID(),32+random.nextFloat(),34+random.nextDouble()));
	}

	private void handle(final Buffer body) {
		log.info("Hadling checking : "+body+"...");
		final JsonObject location = new JsonObject(body.getString(0, body.length()));
//		locations.put(location.getString("userId"), new float[]{location.getNumber("latitude").floatValue(),location.getNumber("longitude").floatValue()});
		log.info("Finished hadling location : "+location);
	}

	
}
