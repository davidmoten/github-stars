# github-stars
Service deployed to AWS API Gateway and Lambda using CF to cache github star counts.

This is an experimental way of deliverying github stars to my [github website](https://davidmoten.github.io).

Using javascript my website used to call github api as an anonymous user to lookup and display the number of github stars (stargazers) for 25 or so projects. The catch is that github only allows 60 calls an hour to its api for anonymous users so a soon-after refresh of the page would lose star counts. Instead of calling github every time, I put a facade in AWS that caches counts and updates those counts once a day per project by using a hash to spread the refresh calls randomly across the 24 hours. This pushes the number of projects that I can support to approximately 60*24 (1440) ignoring hash collisions.

If you call 
```
./deploy.sh
```
this is what you get:

* an artifact bucket in S3 to hold compiled java binaries
* a deployed lambda that returns cached counts and does lookups when required
* an API Gateway that enables CORS and returns plain text from lambda call
* a Rule that calls the lambda once an hour to refresh counts
* an S3 bucket to hold counts per project
* deploys API once stack update completed

