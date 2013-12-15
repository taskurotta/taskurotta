package ru.taskurotta.service.ora.tools;


/**
 * User: moroz
 * Date: 23.05.13
 */
public class PagedQueryBuilder {

    public static String createPagesQuery(String query) {
        StringBuilder pagedSql = new StringBuilder("SELECT t1.* FROM ( SELECT t.*, ROWNUM rnum FROM ( select a1.*, count(*) over() as cnt FROM ( ");
        pagedSql.append(query);
        pagedSql.append(" ) a1) t WHERE ROWNUM <= ? ) t1 WHERE t1.rnum >= ?");
        return pagedSql.toString();
    }
}
