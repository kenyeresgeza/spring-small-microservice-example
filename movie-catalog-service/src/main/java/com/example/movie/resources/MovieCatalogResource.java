package com.example.movie.resources;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.example.movie.config.DbConfig;
import com.example.movie.model.CatalogItem;
import com.example.movie.model.Movie;
import com.example.movie.model.Rating;
import com.example.movie.model.UserRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/catalog")
public class MovieCatalogResource {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    WebClient.Builder builder;

    @Autowired
    DbConfig dbConfig;

    @Value("${my.message}")
    private String message;

    @GetMapping("/config")
    public String hello() {
        return "connection: "+dbConfig.getConnection()+", host: "+dbConfig.getHost()+", port: "+dbConfig.getPort();
    }

    @GetMapping("/{userId}")

    // By connecting to a remote service or resource, you can handle errors that might take a long time to fix.
    @CircuitBreaker(name = "ratingService", fallbackMethod = "fallbackMethodForRatingService")
    // Allows an application to handle predicted transient failures when connecting to services or network resources
    // by transparently retrying a previously failed operation.
    @Retry(name = "ratingService", fallbackMethod = "retryMethodForRatingService")
    // It separates the elements of an application into sets, so that if one of them fails, the rest will continue to work.
    @Bulkhead(name = "ratingService", fallbackMethod = "bulkHeadMethodForRatingService")
    public List<CatalogItem> getCatalogs(@PathVariable("userId") String userId) {

        // discoveryClient.getInstances("service_id");

        List<CatalogItem> catalogItemList = new ArrayList<>();

        UserRating ratings = restTemplate.getForObject("http://movie-rating-service/ratingsdata/users/"+userId,
                UserRating.class);

        for (Rating rating : ratings.getUserRatings()) {

            //Movie movie = restTemplate.getForObject("https://localhost:8082/movies/" + rating.getMovieId(), Movie.class);

            Movie movie = builder.build().get()
                    .uri("http://movie-info-service/movies/" + rating.getMovieId())
                    .retrieve().bodyToMono(Movie.class).block();

            CatalogItem catalogItem = new CatalogItem(movie.getName(), "Test", rating.getRating());

            catalogItemList.add(catalogItem);
        }

        return catalogItemList;
    }

    public ResponseEntity<String> fallbackMethodForRatingService(Exception exception) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("This is the fallback method!");
    }

    public ResponseEntity<String> retryMethodForRatingService(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This is the retry method!");
    }

    public ResponseEntity<String> bulkHeadMethodForRatingService(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This is the bulkhead method!");
    }
}
