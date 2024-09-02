package com.joto.lab.es.test.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.dto.Pit;
import com.joto.lab.es.core.enmus.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *  PitDto
 * @author joey
 * @date 2024-09-02 16:32:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherInfoPit extends Pit {
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDateTime birthday;

    /**
     * address
     */
    @EsField(fieldName = "address", fieldType = EsFieldType.KEYWORD, ignoreAbove = 128)
    private String address;
}