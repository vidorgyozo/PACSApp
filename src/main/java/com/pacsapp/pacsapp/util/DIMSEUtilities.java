package com.pacsapp.pacsapp.util;

import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.net.Status;

import java.lang.reflect.Field;

@Slf4j
public class DIMSEUtilities {

    public static String getStatusText(int status){
        Field[] fields = Status.class.getFields();
        try {
            for (Field field :
                    fields) {
                if ((int)field.get(null) == status) {
                    return field.getName();
                }
            }
        } catch (IllegalAccessException illegalAccessException){
            log.error("Problem while finding status text: {}", status);
        }
        return "" + status;
    }

}
