package com.examprep.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleJsonTest {

    @Test
    void rawObjectKeepsEncodedArray() {
        String json = SimpleJson.rawObject(
                "name", SimpleJson.quoted("boss"),
                "driveFiles", SimpleJson.array(List.of(SimpleJson.object("id", "file-1"))));
        assertEquals("{\"name\":\"boss\",\"driveFiles\":[{\"id\":\"file-1\"}]}", json);
    }

    @Test
    void unescapeJsonStringTurnsEscapedNewlines() {
        assertEquals("BEGIN\nEND", SimpleJson.unescapeJsonString("BEGIN\\nEND"));
    }
}
