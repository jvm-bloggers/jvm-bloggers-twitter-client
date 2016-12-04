#Twitter client

[![Build Status](https://travis-ci.org/jvm-bloggers/jvm-bloggers-twitter-client.svg?branch=master)](https://travis-ci.org/jvm-bloggers/jvm-bloggers-twitter-client)

##Running

You need to **have kafka running** on the adress specified in application.conf (by default localhost:9092)

### Sbt 
 * `sbt run`
 * custom port - `sbt -Dhttp.port=9001 run`
 
## Test twitter account

Integration tests and dev enviroment use this fake account:
https://twitter.com/JvmTest

### Pushing to Docker
* `sbt dockerPush` to build and push
