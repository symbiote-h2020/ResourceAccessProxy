# ResourceAccessProxy (RAP) - running standalone 

RAP can be run in standalone mode which is used when developing RAP plugin.

In this mode there are following constraints:

- RAP can not be used from the core
- Security is turned off
- Resources need to be registered by sending RabbitMQ message to RAP
- RAP needs to be compiled locally

There are also some benefits:

- You can use HTTP for accessing it
- There are no problems with user rights and tokens

## Steps for running it

1. Download RAP from hithub
2. Compile and create Docker image: `./build-local-image.sh`
3. Run RAP, RabbitMQ and Mongo in Docker Swarm: `./startSwarm.sh`
4. Register default resources

    - Open RabbitMQ Management [http://localhost:15672/#/exchanges/%2F/symbIoTe.registrationHandler](http://localhost:15672/#/exchanges/%2F/symbIoTe.registrationHandler)
    - Go to publish message and fill in:

        - Routing key: `symbIoTe.rh.resource.core.register`
        - Payload:

```json
[
  {
    "internalId": "isen1",
    "pluginId": "platform_01",
    "accessPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "filteringPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "resource": {
      "@c": ".StationarySensor",
      "id": "ssid1",
      "name": "DefaultSensor1",
      "description": ["Default sensor for testing RAP"],
      "featureOfInterest": {
        "name": "temperature feature of interest",
        "description": ["measures temperature"],
        "hasProperty": ["temperature"]
      },
      "observesProperty": ["temperature"],
      "locatedAt": {
        "@c": ".WGS84Location",
        "longitude": 13.363782,
        "latitude": 52.513681,
        "altitude": 15,
        "name": "Berlin",
        "description": ["Grosser Tiergarten"]
      },
      "interworkingServiceURL": "https://symbiotedoc.tel.fer.hr/"
    }
  },
  {
    "internalId": "iaid1",
    "pluginId": "platform_01",
    "accessPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "filteringPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "resource": {
      "@c": ".Actuator",
      "id": "sact1",
      "name": "Light 1",
      "description": ["This is light 1"],
      "services": null,
      "capabilities": [
        {
          "name": "OnOffCapabililty",
          "parameters": [
            {
              "name": "on",
              "mandatory": true,
              "datatype": {
                "@c": ".PrimitiveDatatype",
                "baseDatatype": "http://www.w3.org/2001/XMLSchema#boolean"
              }
            }
          ]
        }
      ],
      "locatedAt": {
        "@c": ".WGS84Location",
        "longitude": 2.349014,
        "latitude": 48.864716,
        "altitude": 15,
        "name": "Paris",
        "description": ["This is paris"]
      },
      "interworkingServiceURL": "https://symbiotedoc.tel.fer.hr"
    }
  },
  {
    "internalId": "isrid1",
    "pluginId": "platform_01",
    "accessPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "filteringPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "resource": {
      "@c": ".Service",
      "id": "sserv1",
      "name": "Light service 1",
      "description": ["This is light service 1"],
      "interworkingServiceURL": "https://symbiotedoc.tel.fer.hr",
      "parameters": [
        {
          "name": "inputParam1",
          "mandatory": true,
          "restrictions": [
            {
              "@c": ".LengthRestriction",
              "min": 2,
              "max": 10
            }
          ],
          "datatype": {
            "@c": ".PrimitiveDatatype",
            "isArray": false,
            "baseDatatype": "http://www.w3.org/2001/XMLSchema#string"
          }
        }
      ],
      "resultType": {
        "@c": ".PrimitiveDatatype",
        "isArray": false,
        "baseDatatype": "http://www.w3.org/2001/XMLSchema#string"
      }
    }
  },

  {
    "internalId": "rp_isen1",
    "pluginId": "RapPluginExample",
    "accessPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "filteringPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "resource": {
      "@c": ".StationarySensor",
      "id": "ssid2",
      "name": "DefaultSensor1",
      "description": ["Default sensor for testing RAP"],
      "featureOfInterest": {
        "name": "temperature feature of interest",
        "description": ["measures temperature"],
        "hasProperty": ["temperature"]
      },
      "observesProperty": ["temperature"],
      "locatedAt": {
        "@c": ".WGS84Location",
        "longitude": 13.363782,
        "latitude": 52.513681,
        "altitude": 15,
        "name": "Berlin",
        "description": ["Grosser Tiergarten"]
      },
      "interworkingServiceURL": "https://symbiotedoc.tel.fer.hr/"
    }
  },
  {
    "internalId": "rp_iaid1",
    "pluginId": "RapPluginExample",
    "accessPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "filteringPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "resource": {
      "@c": ".Actuator",
      "id": "sact2",
      "name": "Light 1",
      "description": ["This is light 1"],
      "services": null,
      "capabilities": [
        {
          "name": "OnOffCapabililty",
          "parameters": [
            {
              "name": "on",
              "mandatory": true,
              "datatype": {
                "@c": ".PrimitiveDatatype",
                "baseDatatype": "http://www.w3.org/2001/XMLSchema#boolean"
              }
            }
          ]
        }
      ],
      "locatedAt": {
        "@c": ".WGS84Location",
        "longitude": 2.349014,
        "latitude": 48.864716,
        "altitude": 15,
        "name": "Paris",
        "description": ["This is paris"]
      },
      "interworkingServiceURL": "https://symbiotedoc.tel.fer.hr"
    }
  },
  {
    "internalId": "rp_isrid1",
    "pluginId": "RapPluginExample",
    "accessPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "filteringPolicy": {
      "policyType": "PUBLIC",
      "requiredClaims": {}
    },
    "resource": {
      "@c": ".Service",
      "id": "sserv2",
      "name": "Light service 1",
      "description": ["This is light service 1"],
      "interworkingServiceURL": "https://symbiotedoc.tel.fer.hr",
      "parameters": [
        {
          "name": "inputParam1",
          "mandatory": true,
          "restrictions": [
            {
              "@c": ".LengthRestriction",
              "min": 2,
              "max": 10
            }
          ],
          "datatype": {
            "@c": ".PrimitiveDatatype",
            "isArray": false,
            "baseDatatype": "http://www.w3.org/2001/XMLSchema#string"
          }
        }
      ],
      "resultType": {
        "@c": ".PrimitiveDatatype",
        "isArray": false,
        "baseDatatype": "http://www.w3.org/2001/XMLSchema#string"
      }
    }
  }
]
```

- Click on `Publish message`

5. Sending requests to RAP

This reads one sensor data.

```sh
curl -X GET \
  'http://localhost:8080/rap/Sensor%28%27ssid1%27%29/Observations?$top=1' \
  -H 'Accept: application/json' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Host: localhost:8080' \
  -H 'Postman-Token: 9defd0e2-59c2-47bb-b5b9-5ae44ac549c6,fbb498aa-b8d1-4126-b3a5-9e9fd89b6f71' \
  -H 'User-Agent: PostmanRuntime/7.11.0' \
  -H 'accept-encoding: gzip, deflate' \
  -H 'cache-control: no-cache' \
  -H 'x-auth-size: 0' \
  -H 'x-auth-timestamp: 0'
```

Similar requests and instructions can be found in [here](https://github.com/symbiote-h2020/SymbioteCloud/wiki/3.4-Accessing-the-resource-and-actuating-and-invoking-service-for-default-(dummy)-resources).

6. Stop swarm: `./stopSwarm.sh`

## Tips and tricks

### Show logs from RAP

```
$ docker ps
CONTAINER ID        IMAGE                                       COMMAND                  CREATED             STATUS              PORTS                                                 NAMES
fa3a7d70308e        mongo:3.6                                   "docker-entrypoint.s…"   7 seconds ago       Up 4 seconds        27017/tcp                                             symbiote-cloud_symbiote_mongo.1.hod9tryseua8rbwvz96wlx9e0
3fbb9dc36a6c        symbioteh2020/symbiote-rap:3.1.0-SNAPSHOT   "java -DSPRING_BOOT_…"   9 seconds ago       Up 6 seconds        8103/tcp                                              symbiote-cloud_symbiote_rap.1.qzlnocs4a5592eebhjc79kupg
e8a3b49d1ef4        rabbitmq:3-management                       "docker-entrypoint.s…"   9 seconds ago       Up 6 seconds        4369/tcp, 5671-5672/tcp, 15671-15672/tcp, 25672/tcp   symbiote-cloud_symbiote_rabbitmq.1.ovsx5s03yy8lvn94ui8e72g9t
```

From previous console printout we can see that RAP has container id `3fbb9dc36a6c`.

```
$ docker logs 3fbb9dc36a6c -f
Waiting for symbiote_mongo:27017
Waiting for symbiote_mongo:27017
2019-05-02 09:51:03.595  INFO [ResourceAccessProxy,,,] 1 --- [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration' of type [org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration$$EnhancerBySpringCGLIB$$ef202447] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.1.4.RELEASE)

2019-05-02 09:51:04.191  INFO [ResourceAccessProxy,,,] 1 --- [           main] c.c.c.ConfigServicePropertySourceLocator : Fetching config from server at : http://localhost:8888
2019-05-02 09:51:04.399  INFO [ResourceAccessProxy,,,] 1 --
...
```
