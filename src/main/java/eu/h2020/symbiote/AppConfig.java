/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


/**
 *
 * @author Matteo Pardi
 */
@Configuration
class AppConfig extends AbstractMongoConfiguration {

    @Value("${rap.mongo.dbname}")
    private String databaseName;

    @Value("${rap.mongo.host}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port}")
    private int mongoPort;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Mongo mongo() throws Exception {
        return new Mongo(mongoHost, mongoPort);
    }

    @Bean
    @Override
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(new MongoClient(mongoHost), getDatabaseName());
    }
}
