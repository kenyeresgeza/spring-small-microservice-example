package com.example.rating.resources;

import com.example.rating.model.Rating;
import com.example.rating.model.UserRating;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/ratingsdata")
public class RatingsDataResource {

    @RequestMapping("/{movieId}")
    public Rating getRating(@PathVariable("movieId") String movieId) {
        return new Rating(movieId, 4);
    }

    @RequestMapping("users/{userId}")
    public UserRating getUserRating(@PathVariable("userId") String userId) {
        List<Rating> ratings = Arrays.asList(
                new Rating("1234", 4),
                new Rating("5678", 3),
                new Rating("9912", 10),
                new Rating("8854", 8),
                new Rating("7886", 9)
        );

        UserRating userRating = new UserRating();
        userRating.setUserRatings(ratings);

        return userRating;
    }

}
