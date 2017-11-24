package com.github.davidmoten.apig.example;

import java.io.IOException;
import java.util.Map;

import org.kohsuke.github.GitHub;

import com.amazonaws.services.lambda.runtime.Context;
import com.github.davidmoten.aws.helper.StandardRequestBodyPassThrough;

public class Handler {

    public String handle(Map<String, Object> input, Context context) {

        // expects full request body passthrough from api gateway integration
        // request
        StandardRequestBodyPassThrough request = StandardRequestBodyPassThrough.from(input);

        String name = request.queryStringParameter("name")
                .orElseThrow(() -> new IllegalArgumentException("parameter 'name' not found"));

        try {
            GitHub g = GitHub.connectAnonymously();
            return String.valueOf(g.getRepository(name).getStargazersCount());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
