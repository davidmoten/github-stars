package com.github.davidmoten.apig.example;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kohsuke.github.GitHub;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.github.davidmoten.aws.helper.StandardRequestBodyPassThrough;

public class Handler {

    public String handle(Map<String, Object> input, Context context) {

        // expects full request body passthrough from api gateway integration
        // request
        StandardRequestBodyPassThrough request = StandardRequestBodyPassThrough.from(input);

        String name = request.queryStringParameter("name")
                .orElseThrow(() -> new IllegalArgumentException("parameter 'name' not found"));

        if ("scheduled".equals(name)) {
            String bucketName = System.getenv("BUCKET_NAME");
            AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            long now = System.currentTimeMillis();
            int hourOfDay = hourOfDay(now);
            s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withMaxKeys(1000))
                    .getObjectSummaries() //
                    .stream() //
                    .map(o -> {
                        boolean expired = now - o.getLastModified().getTime() >= TimeUnit.HOURS
                                .toMillis(24);
                        boolean isSlot = hourOfDay == Math.abs(o.getKey().hashCode()) % 24;
                        int count;
                        if (expired && isSlot) {
                            try {
                                GitHub g = GitHub.connectAnonymously();
                                count = g.getRepository(o.getKey()).getStargazersCount();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            s3.putObject(bucketName, o.getKey(), String.valueOf(count));
                            return count;
                        } else {
                            s3.getObject(bucketName, o.getKey()).getObjectContent();
                            return 123;
                        }
                    });
            return "0";
        } else {
            return "0";
        }
    }

    private int hourOfDay(long now) {
        return (int) ((now % TimeUnit.DAYS.toMillis(1)) / TimeUnit.HOURS.toMillis(1));
    }
}
