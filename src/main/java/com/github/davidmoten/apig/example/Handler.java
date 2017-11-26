package com.github.davidmoten.apig.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.kohsuke.github.GitHub;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.github.davidmoten.aws.helper.StandardRequestBodyPassThrough;

public class Handler {

    public String handle(Map<String, Object> input, Context context) {
        LambdaLogger log = context.getLogger();

        // expects full request body passthrough from api gateway integration
        // request
        StandardRequestBodyPassThrough request = StandardRequestBodyPassThrough.from(input);

        String name = request.queryStringParameter("name").orElse("scheduled");
        String bucketName = System.getenv("BUCKET_NAME");
        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        GitHub g;
        try {
            g = GitHub.connectAnonymously();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        if ("scheduled".equals(name)) {
            long now = System.currentTimeMillis();
            int hourOfDay = hourOfDay(now);
            s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withMaxKeys(1000))
                    .getObjectSummaries() //
                    .stream() //
                    .forEach(o -> {
                        boolean expired = now - o.getLastModified().getTime() >= TimeUnit.HOURS
                                .toMillis(24);
                        boolean isSlot = hourOfDay == Math.abs(o.getKey().hashCode()) % 24;
                        if (expired && isSlot) {
                            try {
                                int count = g.getRepository(o.getKey()).getStargazersCount();
                                s3.putObject(bucketName, o.getKey(), String.valueOf(count));
                                log.log("set count for project " + o.getKey() + " to " + count);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
            return "0";
        } else if (s3.doesObjectExist(bucketName, name)) {
            S3Object object = s3.getObject(bucketName, name);
            return toString(object.getObjectContent());
        } else {
            s3.putObject(bucketName, name, "0");
            return "0";
        }
    }

    private static String toString(S3ObjectInputStream is) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private int hourOfDay(long now) {
        return (int) ((now % TimeUnit.DAYS.toMillis(1)) / TimeUnit.HOURS.toMillis(1));
    }
}
