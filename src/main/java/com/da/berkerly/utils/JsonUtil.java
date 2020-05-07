package com.da.berkerly.utils;


import com.da.berkerly.data.Data;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonUtil {

    public static String data2Json(Data data){

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr =null;
        try {
            jsonStr = mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonStr;
    }

    public static Data json2data(String jsonStr) {

        ObjectMapper mapper = new ObjectMapper();
        Data data = null;
        try {
            data = mapper.readValue(jsonStr, Data.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

}
