package ru.taskurotta.service.hz.storage

import com.mongodb.BasicDBObject
import com.mongodb.DB
import org.slf4j.LoggerFactory
import java.util.*


interface StringSetCounter {

    val size: Long
    fun add(customId: String)
    fun findUniqueItems(supposedUniqueList: List<String>): List<String>
}

private val logger = LoggerFactory.getLogger(MongoStringSetCounter::class.java)

public class MongoStringSetCounter(name: String = "StringCounter", db: DB): StringSetCounter {

    val collection = db.getCollection(name)
    val incQuery = inc("val", 1)

    override val size: Long
        get() {
            return collection.count
        }

    override fun add(customId: String) {
        collection.update(eq("_id", customId), incQuery, true, false);
    }

    override fun findUniqueItems(supposedUniqueList: List<String>): List<String> {

        val set = HashSet(supposedUniqueList)

        val dbCursor = collection.find().batchSize(500)
        while (dbCursor.hasNext()) {
            val document = dbCursor.next()
            val customId = document.get("_id") as String

            if (!set.remove(customId)) {
                continue
            }

            val counter = document.get("val") as Int
            if (counter != 1) {
                logger.warn("Counter > 1. customId = {} counter = {}",
                        customId, counter)
            }
        }

        return set.toList()
    }

}

private fun eq(key: String, value: Any): BasicDBObject {
    return BasicDBObject(key, value);
}

private fun inc(key: String, value: Any): BasicDBObject {
    return eq("\$inc", eq(key, value))
}


//fun main(args: Array<String>) {
//    val db = MongoClient("localhost").getDB("taskurotta");
//
//    val staticValue = MongoStringSetCounter(db = db) as StringSetCounter;
//
//    println("Old size is ${staticValue.size}")
//    staticValue.add("testId")
//    println("New size is ${staticValue.size}")
//
//    println("find unique items: " + staticValue.findUniqueItems(arrayOf("testId", "xxx").asList()))
//}