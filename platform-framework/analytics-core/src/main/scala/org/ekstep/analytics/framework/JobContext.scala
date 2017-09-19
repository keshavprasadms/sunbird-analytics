package org.ekstep.analytics.framework

import org.apache.spark.rdd.RDD
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Buffer

/**
 * @author Santhosh
 */
object JobContext {

    var parallelization: Int = 10;

    var deviceMapping: Map[String, String] = Map();

    var jobName: String = "default";

    val rddList: Buffer[AnyRef] = ListBuffer();

    def recordRDD[T](rdd: RDD[T]) {
        rddList += rdd;
    }

    def cleanUpRDDs() = {
        println("rddList.length: "+ rddList.length)
        rddList.foreach { x =>
            
            val rdd = x.asInstanceOf[RDD[AnyRef]]
            println("rdd.count after 1'val rdd = x.asInstanceOf[RDD[AnyRef]]': "+ rdd.count)
            if (rdd != null && rdd.getStorageLevel.useMemory) {
                rdd.unpersist(true);
            }
        }
        rddList.clear();
    }
}