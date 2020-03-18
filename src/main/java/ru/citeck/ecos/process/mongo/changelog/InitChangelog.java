package ru.citeck.ecos.process.mongo.changelog;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.DB;

/**
 *  MongoBee default changelog for exclude search folder on start
 */
@ChangeLog
public class InitChangelog {

    @ChangeSet(order = "001", id = "initChangelog", author = "olegraskin")
    public void initChangelog(DB db){
    }
}
