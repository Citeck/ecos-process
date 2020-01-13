package ru.citeck.ecos.process.web.rest;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    Session session;

    @GetMapping("/list")
    public String getJournalPrefs(@RequestParam(defaultValue = "default param") String param) {

        ResultSet rs = session.execute("SELECT * FROM system_schema.tables");
        System.out.println(rs);

        rs.forEach(row -> {
            for (int i = 0; i < rs.getColumnDefinitions().size(); i++) {
                System.out.println(row.getObject(i));
            }
            System.out.println();
        });

        return "TEST RESULT: " + param;
    }
}
