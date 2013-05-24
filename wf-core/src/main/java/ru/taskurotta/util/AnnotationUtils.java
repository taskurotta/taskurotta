package ru.taskurotta.util;

import java.lang.annotation.Annotation;

/**
 * User: jedy
 * Date: 11.12.12 12:36
 */
public class AnnotationUtils {

    /**
     * Copied from {@link org.springframework.core.annotation.AnnotationUtils}.
     * Find a single {@link java.lang.annotation.Annotation} of <code>annotationType</code> from the supplied {@link Class},
     * traversing its interfaces and superclasses if no annotation can be found on the given class itself.
     * <p>This method explicitly handles class-level annotations which are not declared as
     * {@link java.lang.annotation.Inherited inherited} <i>as well as annotations on interfaces</i>.
     * <p>The algorithm operates as follows: Searches for an annotation on the given class and returns
     * it if found. Else searches all interfaces that the given class declares, returning the annotation
     * from the first matching candidate, if any. Else proceeds with introspection of the superclass
     * of the given class, checking the superclass itself; if no annotation found there, proceeds
     * with the interfaces that the superclass declares. Recursing up through the entire superclass
     * hierarchy if no match is found.
     *
     * @param clazz          the class to look for annotations on
     * @param annotationType the annotation class to look for
     * @return the class with annotation found, or <code>null</code> if none found
     */
    public static <A extends Annotation> Class<?> findAnnotatedClass(Class<?> clazz, Class<A> annotationType) {
        if(clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        Annotation annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            return clazz;
        }
        Class theClass;
        for (Class<?> ifc : clazz.getInterfaces()) {
            theClass = findAnnotatedClass(ifc, annotationType);
            if (theClass != null) {
                return theClass;
            }
        }
        if (!Annotation.class.isAssignableFrom(clazz)) {
            for (Annotation ann : clazz.getAnnotations()) {
                theClass = findAnnotatedClass(ann.annotationType(), annotationType);
                if (theClass != null) {
                    return theClass;
                }
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass == null || superClass == Object.class) {
            return null;
        }
        return findAnnotatedClass(superClass, annotationType);
    }

}
