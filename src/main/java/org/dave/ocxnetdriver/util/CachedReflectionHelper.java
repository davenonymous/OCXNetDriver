package org.dave.ocxnetdriver.util;

import org.dave.ocxnetdriver.Logz;

import java.lang.reflect.Field;
import java.util.HashMap;

public class CachedReflectionHelper {
    private static HashMap<Class, HashMap<String, Field>> fieldCache = new HashMap<>();

    public static <T> T getFieldValue(Class T, Object obj, String fieldName) {
        Class objClass = obj.getClass();
        if(!fieldCache.containsKey(objClass)) {
            fieldCache.put(obj.getClass(), new HashMap<>());
        }

        HashMap<String, Field> innerMap = fieldCache.get(objClass);
        if(!innerMap.containsKey(fieldName)) {
            try {
                Field field = getInheritedField(objClass, fieldName);
                field.setAccessible(true);
                innerMap.put(fieldName, field);
            } catch (NoSuchFieldException e) {
                Logz.warn("Object %s of class %s has no field with name '%s'", obj, obj.getClass().getCanonicalName(), fieldName);
                return null;
            }
        }

        try {
            return (T) innerMap.get(fieldName).get(obj);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field getInheritedField(Class objClass, String fieldName) throws NoSuchFieldException {
        for(Class clz : InheritanceUtil.getInheritance(objClass)) {
            try {
                return clz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                continue;
            }

        }

        throw new NoSuchFieldException(fieldName);
    }
}
