package com.joto.lab.es.test;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import co.elastic.clients.elasticsearch._types.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.joto.lab.es.core.dto.PagingDto;
import com.joto.lab.es.core.dto.PitDto;
import com.joto.lab.es.core.utils.JsonUtil;
import com.joto.lab.es.test.dto.TeacherInfoPaging;
import com.joto.lab.es.test.dto.TeacherInfoPit;
import com.joto.lab.es.test.entity.TeacherInfo;
import com.joto.lab.es.test.service.TeacherInfoServiceImpl;
import jakarta.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @authro joey
 * @date 2024/9/2 16:33
 */
@Slf4j
@SpringBootTest
public class TeacherInfoTest {

    private static String INDEX_NAME = "teacher-info";

    private TeacherInfoServiceImpl teacherInfoService;

    @Autowired
    public void setTeacherInfoService(TeacherInfoServiceImpl teacherInfoService) {
        this.teacherInfoService = teacherInfoService;
    }

    @Test
    public void saveTest() throws IOException {
        for (int i = 0; i < 10; i++) {
            TeacherInfo teacherInfo = generateInfo(i);
            Result save = teacherInfoService.save(INDEX_NAME, IdUtil.fastSimpleUUID(), teacherInfo);
            log.info("save:{}", save);
        }
    }


    @Test
    public void bulkTest() throws IOException {
        int size = 5555;
        List<TeacherInfo> list = new ArrayList<>(size + 1);
        for (int i = 0; i < size; i++) {
            TeacherInfo teacherInfo = generateInfo(i);
            list.add(teacherInfo);
        }

        Integer success = teacherInfoService.bulk(INDEX_NAME, list);
        log.info("bulk:{}", success);
    }

    public void getTest() throws IOException {
        String id = "123";
        TeacherInfo teacherInfo = teacherInfoService.getById(INDEX_NAME, id, TeacherInfo.class);
        log.info("{}", JsonUtil.toJsonStr(teacherInfo));
    }

    public void deleteTest() throws IOException {
        String id = "123";
        Result result = teacherInfoService.delete(INDEX_NAME, id);
        log.info("del:{}", result);
    }

    @Test
    public void pagingTest() throws IOException {
        TeacherInfoPaging paging = new TeacherInfoPaging();
        paging.setSize(20);
        paging.setIndex(0);

        paging.setGender(RandomUtil.randomBoolean() + "");

        PagingDto<TeacherInfo> pagingDto = teacherInfoService.paging(INDEX_NAME, paging, TeacherInfo.class);
        log.info("{}", JsonUtil.toJsonStr(pagingDto));
    }

    @Test
    public void pitTest() throws IOException {
        TeacherInfoPit pit = new TeacherInfoPit();
        pit.setGender(RandomUtil.randomBoolean() + "");

        PitDto<TeacherInfo> pitDto = teacherInfoService.pit(INDEX_NAME, pit, TeacherInfo.class);
        log.debug("total: {}, more:{}", pitDto.getTotal(), pitDto.isMore());

        while (pitDto.isMore()) {
            pit.setPit(pitDto.getPit());
            pit.setSorts(pitDto.getSorts());

            pitDto = teacherInfoService.pit(INDEX_NAME, pit, TeacherInfo.class);
            log.debug("-> total: {}, more:{}", pitDto.getTotal(), pitDto.isMore());
        }
    }

    private TeacherInfo generateInfo(int i) {
        TeacherInfo teacherInfo = new TeacherInfo();
        teacherInfo.setId(RandomUtil.randomString(8) + i);
        teacherInfo.setName("teacher" + i);
        teacherInfo.setAddress(RandomUtil.randomString(16));
        teacherInfo.setGender(RandomUtil.randomBoolean() + "");
        teacherInfo.setBirthday(LocalDateTime.now());
        return teacherInfo;
    }
}
