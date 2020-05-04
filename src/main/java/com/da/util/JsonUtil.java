package com.da.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.IOException;
import java.util.Date;

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

}
