package ru.citeck.ecos.process.mongo.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;

@ChangeLog(order = "001")
public class InitChangelogMongoCk {

    @ChangeSet(order = "001", id = "initChangelog", author = "olegraskin")
    public void initChangelog(MongoDatabase db) {
        // empty
    }

    @ChangeSet(order = "002", id = "addEnabledFlagForProcDef", author = "pavel.simonov")
    public void addEnabledFlagForProcDef(MongoDatabase db) {
        db.getCollection("process_def").updateMany(new BasicDBObject(),
            new BasicDBObject("$set", new BasicDBObject("enabled", true))
        );
    }
}
