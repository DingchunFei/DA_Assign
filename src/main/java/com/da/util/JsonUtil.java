package com.da.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonUtil {

    public static String long2Json(Long currentTime){

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr =null;
        try {
            jsonStr = mapper.writeValueAsString(currentTime);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonStr;
    }

    public static Long json2long(String jsonStr) {

        ObjectMapper mapper = new ObjectMapper();
        Long currentTime = null;
        try {
            currentTime = mapper.readValue(jsonStr, Long.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentTime;
    }

    public static String map2Json(Map<Date,String> map){
        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<Date, String>> entries = map.entrySet();
        int count = 0;
        for (Map.Entry<Date, String> entry: entries) {
            sb.append(long2Json(entry.getKey().getTime())).append(":").append(entry.getValue());
            if(count < map.size()-1) {
                sb.append(",");
            }
            count++;
        }
        return sb.toString();
    }

    public static Map<Date,String> json2Map(String str){
        String[] str1 = str.split(",");
        Map<Date,String> map = new HashMap<>();
        for (int i = 0; i < str1.length; i++) {
            String[] str2 = str1[i].split(":");
            map.put(new Date(json2long(str2[0])),str2[1]);
        }
        return map;
    }

}
