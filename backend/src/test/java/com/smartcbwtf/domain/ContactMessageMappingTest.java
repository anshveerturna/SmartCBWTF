package com.smartcbwtf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContactMessageMappingTest {

    @Test
    void messageMapsToPostgresTextNotLargeObject() throws NoSuchFieldException {
        Field messageField = ContactMessage.class.getDeclaredField("message");
        Column column = messageField.getAnnotation(Column.class);

        assertFalse(messageField.isAnnotationPresent(Lob.class));
        assertEquals("TEXT", column.columnDefinition());
    }
}
