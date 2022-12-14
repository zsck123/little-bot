package com.zsck.bot.http.academic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsck.bot.http.academic.pojo.ClassMap;
import com.zsck.bot.http.academic.service.ClassNameService;
import com.zsck.bot.http.academic.mapper.ClassMapMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author QQ:825352674
 * @date 2022/7/23 - 15:52
 */
@Service
public class ClassNameServiceImpl extends ServiceImpl<ClassMapMapper , ClassMap>
        implements ClassNameService {
    private Map<Integer , String> classNameMap;

    @PostConstruct
    private void init(){
        classNameMap = new HashMap<>();
        List<Map<String , Object>> list = baseMapper.selectMaps(null);
        list.forEach(el -> classNameMap.put(((Integer) el.get("id")), ((String) el.get("class_name"))));
    }


    @Override
    public String getClassName(Integer lessonId) {
        return classNameMap.get(lessonId);
    }

}
