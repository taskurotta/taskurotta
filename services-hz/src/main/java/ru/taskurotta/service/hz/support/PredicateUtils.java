package ru.taskurotta.service.hz.support;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import java.util.List;

/**
 * Created on 03.06.2015.
 */
public class PredicateUtils {

    private static final String WILDCARD_SYMBOL = "%";

    public static Predicate getStartsWith(String field, String value) {
        if (field == null || value == null) {
            return null;
        }
        return Predicates.like(field, value.trim() + WILDCARD_SYMBOL);
    }

    public static Predicate getEqual(String field, Comparable value) {
        if (field == null || value == null) {
            return null;
        }
        return Predicates.equal(field, value);
    }

    public static Predicate getLessThen(String field, long positiveValue) {
        if (field == null || positiveValue < 0) {
            return null;
        }
        return Predicates.between(field, 0l, positiveValue);
    }

    public static Predicate getMoreThen(String field, long positiveValue) {
        if (field == null || positiveValue < 0) {
            return null;
        }
        return Predicates.between(field, positiveValue, Long.MAX_VALUE);
    }

    public static Predicate combineWithAndCondition(List<Predicate> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return null;
        }
        return Predicates.and(predicates.toArray(new Predicate[predicates.size()]));
    }


}
