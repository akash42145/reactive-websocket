package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ReactiveWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveWebsocketApplication.class, args);
	}

}

@Configuration
@Log4j2
class GreetingWebSocketConfiguration {
	
	@Bean
	public SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
		return new SimpleUrlHandlerMapping(Map.of("/ws/wishings", wsh), 10);
	}
	
	@Bean 
	public WebSocketHandler webSocketHandler(GreetingService greetingService) {
		return new WebSocketHandler() {
			
			@Override
			public Mono<Void> handle(WebSocketSession session) {
				Flux<WebSocketMessage> receive = session.receive();
				Flux<String> names = receive.map(WebSocketMessage::getPayloadAsText);
				Flux<GreetingRequest> requestFlux = names.map(GreetingRequest::new);
				Flux<GreetingResponse> greetingResponseFlux = requestFlux.flatMap(greetingService::greetMany);
				Flux<String> map = greetingResponseFlux.map(GreetingResponse::getMessage);
				
				Flux<WebSocketMessage> webSocketMessageFlux = map.map(session::textMessage)
						.doOnEach(signalConsumer -> log.info(signalConsumer.getType()))
						.doFinally(singnal -> log.info("finally :"+ singnal.toString()));
				return session.send(webSocketMessageFlux);
			}
		};
	}
	
	@Bean
	WebSocketHandlerAdapter webSocketHandlerAdapter() {
		return new WebSocketHandlerAdapter();
	}
}

@Service
class GreetingService{
	
	private GreetingResponse greet(String name) {
		return new GreetingResponse("Hello " +name +" @"+ Instant.now());
	}
	
	Flux<GreetingResponse> greetMany(GreetingRequest request){
		return Flux.fromStream(Stream.generate(() -> greet(request.getName()))).delayElements(Duration.ofSeconds(1));
	}
	
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse{
	private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest{
	private String name;
}
