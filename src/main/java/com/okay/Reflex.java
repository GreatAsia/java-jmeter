package com.okay;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author zhou
 * @date 2019/12/30
 */
public class Reflex {


    /**
     * 获取无参构造函数
     *
     * @param className
     * @return
     */
    public static Object createObject(String className) {
        Class[] pareTyples = new Class[]{};
        Object[] pareVaules = new Object[]{};

        try {
            Class r = Class.forName(className);
            return createObject(r, pareTyples, pareVaules);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取无参构造方法
     *
     * @param clazz
     * @return
     */
    public static Object createObject(Class clazz) {
        Class[] pareTyple = new Class[]{};
        Object[] pareVaules = new Object[]{};

        return createObject(clazz, pareTyple, pareVaules);
    }

    /**
     * 获取一个参数的构造函数  已知className
     *
     * @param className
     * @param pareTyple
     * @param pareVaule
     * @return
     */
    public static Object createObject(String className, Class pareTyple, Object pareVaule) {

        Class[] pareTyples = new Class[]{pareTyple};
        Object[] pareVaules = new Object[]{pareVaule};

        try {
            Class r = Class.forName(className);
            return createObject(r, pareTyples, pareVaules);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 获取单个参数的构造方法 已知类
     *
     * @param clazz
     * @param pareTyple
     * @param pareVaule
     * @return
     */
    public static Object createObject(Class clazz, Class pareTyple, Object pareVaule) {
        Class[] pareTyples = new Class[]{pareTyple};
        Object[] pareVaules = new Object[]{pareVaule};

        return createObject(clazz, pareTyples, pareVaules);
    }

    /**
     * 获取多个参数的构造方法 已知className
     *
     * @param className
     * @param pareTyples
     * @param pareVaules
     * @return
     */
    public static Object createObject(String className, Class[] pareTyples, Object[] pareVaules) {
        try {
            Class r = Class.forName(className);
            return createObject(r, pareTyples, pareVaules);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 获取构造方法
     *
     * @param clazz
     * @param pareTyples
     * @param pareVaules
     * @return
     */
    public static Object createObject(Class clazz, Class[] pareTyples, Object[] pareVaules) {
        try {
            Constructor ctor = clazz.getDeclaredConstructor(pareTyples);
            ctor.setAccessible(true);
            return ctor.newInstance(pareVaules);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 获取多个参数的方法
     *
     * @param obj
     * @param methodName
     * @param pareTyples
     * @param pareVaules
     * @return
     */
    public static Object invokeInstanceMethod(Object obj, String methodName, Class[] pareTyples, Object[] pareVaules) {
        if (obj == null) {
            return null;
        }

        try {
            //调用一个private方法 //在指定类中获取指定的方法
            Method method = obj.getClass().getDeclaredMethod(methodName, pareTyples);
            method.setAccessible(true);
            return method.invoke(obj, pareVaules);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取一个参数的方法
     *
     * @param obj
     * @param methodName
     * @param pareTyple
     * @param pareVaule
     * @return
     */
    public static Object invokeInstanceMethod(Object obj, String methodName, Class pareTyple, Object pareVaule) {
        Class[] pareTyples = {pareTyple};
        Object[] pareVaules = {pareVaule};

        return invokeInstanceMethod(obj, methodName, pareTyples, pareVaules);
    }

    /**
     * 获取无参方法
     *
     * @param obj
     * @param methodName
     * @return
     */
    public static Object invokeInstanceMethod(Object obj, String methodName) {
        Class[] pareTyples = new Class[]{};
        Object[] pareVaules = new Object[]{};

        return invokeInstanceMethod(obj, methodName, pareTyples, pareVaules);
    }


    /**
     * 无参静态方法
     *
     * @param className
     * @param method_name
     * @return
     */
    public static Object invokeStaticMethod(String className, String method_name) {
        Class[] pareTyples = new Class[]{};
        Object[] pareVaules = new Object[]{};

        return invokeStaticMethod(className, method_name, pareTyples, pareVaules);
    }

    /**
     * 获取一个参数的静态方法
     *
     * @param className
     * @param method_name
     * @param pareTyple
     * @param pareVaule
     * @return
     */
    public static Object invokeStaticMethod(String className, String method_name, Class pareTyple, Object pareVaule) {
        Class[] pareTyples = new Class[]{pareTyple};
        Object[] pareVaules = new Object[]{pareVaule};

        return invokeStaticMethod(className, method_name, pareTyples, pareVaules);
    }

    /**
     * 获取多个参数的静态方法
     *
     * @param className
     * @param method_name
     * @param pareTyples
     * @param pareVaules
     * @return
     */
    public static Object invokeStaticMethod(String className, String method_name, Class[] pareTyples, Object[] pareVaules) {
        try {
            Class obj_class = Class.forName(className);
            return invokeStaticMethod(obj_class, method_name, pareTyples, pareVaules);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 无参静态方法
     *
     * @param method_name
     * @return
     */
    public static Object invokeStaticMethod(Class clazz, String method_name) {
        Class[] pareTyples = new Class[]{};
        Object[] pareVaules = new Object[]{};

        return invokeStaticMethod(clazz, method_name, pareTyples, pareVaules);
    }

    /**
     * 一个参数静态方法
     *
     * @param clazz
     * @param method_name
     * @param classType
     * @param pareVaule
     * @return
     */
    public static Object invokeStaticMethod(Class clazz, String method_name, Class classType, Object pareVaule) {
        Class[] classTypes = new Class[]{classType};
        Object[] pareVaules = new Object[]{pareVaule};

        return invokeStaticMethod(clazz, method_name, classTypes, pareVaules);
    }

    /**
     * 多个参数的静态方法
     *
     * @param clazz
     * @param method_name
     * @param pareTyples
     * @param pareVaules
     * @return
     */
    public static Object invokeStaticMethod(Class clazz, String method_name, Class[] pareTyples, Object[] pareVaules) {
        try {
            Method method = clazz.getDeclaredMethod(method_name, pareTyples);
            method.setAccessible(true);
            return method.invoke(null, pareVaules);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static Object getFieldObject(String className, Object obj, String filedName) {
        try {
            Class obj_class = Class.forName(className);
            return getFieldObject(obj_class, obj, filedName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getFieldObject(Class clazz, Object obj, String filedName) {
        try {
            Field field = clazz.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static void setFieldObject(Class clazz, Object obj, String filedName, Object filedVaule) {
        try {
            Field field = clazz.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(obj, filedVaule);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setFieldObject(String className, Object obj, String filedName, Object filedVaule) {
        try {
            Class obj_class = Class.forName(className);
            setFieldObject(obj_class, obj, filedName, filedVaule);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static Object getStaticFieldObject(String className, String filedName) {
        return getFieldObject(className, null, filedName);
    }

    public static Object getStaticFieldObject(Class clazz, String filedName) {
        return getFieldObject(clazz, null, filedName);
    }

    public static void setStaticFieldObject(String classname, String filedName, Object filedVaule) {
        setFieldObject(classname, null, filedName, filedVaule);
    }

    public static void setStaticFieldObject(Class clazz, String filedName, Object filedVaule) {
        setFieldObject(clazz, null, filedName, filedVaule);
    }


}
