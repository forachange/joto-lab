package com.joto.lab.es.test.entity;

import com.fasterxml.jackson.databind.annotation.*;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.annotations.EsIndex;
import com.joto.lab.es.core.dto.EsId;
import com.joto.lab.es.core.enmus.EsAnalyzer;
import com.joto.lab.es.core.enmus.EsFieldType;
import com.joto.lab.es.core.serializer.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EsMybatisPlugin Generated
 * @author joey
 * @date 2024-09-02 11:34:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EsIndex(name = "teacher-info")
public class TeacherInfo implements EsId {
    /**
     * id
     */
    @EsField(fieldName = "id", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)
    private String id;

    /**
     * name
     */
    @EsField(fieldName = "name", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)
    private String name;

    /**
     * gender
     */
    @EsField(fieldName = "gender", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)
    private String gender;

    /**
     * birthday
     */
    @EsField(fieldName = "birthday", fieldType = EsFieldType.DATE)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = ZoneDateTimeDeserializer.class)
    private LocalDateTime birthday;

    /**
     * address
     */
    @EsField(fieldName = "address", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)
    private String address;
}