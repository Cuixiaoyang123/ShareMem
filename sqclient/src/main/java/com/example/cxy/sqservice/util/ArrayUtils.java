package com.example.cxy.sqservice.util;

import java.util.Collection;

/**
 * @author zgx
 **/
public class ArrayUtils {
    /**
     * 判断是否为空
     *
     * @param collection
     * @return
     */
    public static boolean empty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean empty(Object[] array){
        return array == null || array.length == 0;
    }


    public static int length(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (isArray(obj)) {
            return ((Object[]) obj).length;
        }
        if (isCollection(obj)) {
            return ((Collection<?>) obj).size();
        }
        return -1;
    }

    public static boolean isArray(Object object) {
        if (object == null) {
            return false;
        }
        return object.getClass().isArray();
    }

    public static boolean isCollection(Object object) {
        return object instanceof Collection;
    }
}
