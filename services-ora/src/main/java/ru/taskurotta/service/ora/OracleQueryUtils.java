package ru.taskurotta.service.ora;

/**
 * Created on 15.06.2015.
 */
public class OracleQueryUtils {

    public static String createPagesQuery(String query) {
        return "SELECT t1.* FROM ( SELECT t.*, ROWNUM rnum FROM ( select a1.*, count(*) over() as cnt FROM ( " +
                query +
                " ) a1) t WHERE ROWNUM <= ? ) t1 WHERE t1.rnum >= ?";
    }
}
