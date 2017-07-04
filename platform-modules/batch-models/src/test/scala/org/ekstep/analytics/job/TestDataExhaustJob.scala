package org.ekstep.analytics.job

import org.ekstep.analytics.framework.JobConfig
import org.ekstep.analytics.framework.Fetcher
import org.ekstep.analytics.framework.Query
import org.ekstep.analytics.framework.Dispatcher
import org.ekstep.analytics.model.SparkSpec
import org.ekstep.analytics.framework.util.JSONUtils
import com.datastax.spark.connector._
import org.ekstep.analytics.framework.util.CommonUtil
import org.joda.time.DateTime
import org.ekstep.analytics.util.Constants
import com.datastax.spark.connector.cql.CassandraConnector
import org.ekstep.analytics.framework.util.S3Util
import java.io.File
import org.ekstep.analytics.util.RequestFilter
import org.ekstep.analytics.util.RequestConfig
import org.ekstep.analytics.util.JobRequest
import org.apache.commons.io.FileUtils

class TestDataExhaustJob extends SparkSpec(null) {

    private def preProcess() {
        CassandraConnector(sc.getConf).withSessionDo { session =>
            session.execute("TRUNCATE platform_db.job_request");
        }
        FileUtils.copyDirectory(new File("src/test/resources/data-exhaust/test"), new File("src/test/resources/data-exhaust-package/"))
    }

    it should "execute DataExhaustJob job from local data and won't throw any Exception" in {

        preProcess()

        val requests = Array(
            JobRequest("partner1", "1234", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2016-11-19", "2016-11-20", Option(List("becb887fe82f24c644482eb30041da6d88bd8150")), Option(List("OE_INTERACT", "GE_INTERACT")), None, None))),
                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None));

        sc.makeRDD(requests).saveToCassandra(Constants.PLATFORM_KEY_SPACE_NAME, Constants.JOB_REQUEST)

        val config = """{"search":{"type":"local","queries":[{"file":"src/test/resources/data-exhaust/creation-raw/*"}]},"model":"org.ekstep.analytics.model.DataExhaustJobModel","output":[{"to":"file","params":{"file": "/tmp/dataexhaust"}}],"parallelization":8,"appName":"Data Exhaust","deviceMapping":false,"modelParams":{}, "exhaustConfig":{"eks-consumption-raw":{"events":["DEFAULT"],"eventConfig":{"DEFAULT":{"eventType":"ConsumptionRaw","searchType":"local","fetchConfig":{"params":{"file":"src/test/resources/data-exhaust/consumption-raw/*"}},"filterMapping":{"tags":{"name":"genieTag","operator":"IN"}}}}}}}"""
        DataExhaustJob.main(config)(Option(sc));
    }

    it should "test consumption summary data" in {
        preProcess()

        val requests = Array(
            JobRequest("client-key1", "requestID1", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2017-06-18", "2017-06-18", Option(List()), Option(List("ME_SESSION_SUMMARY")), None, None), Option("eks-consumption-summary"))),
                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None))

        sc.makeRDD(requests).saveToCassandra(Constants.PLATFORM_KEY_SPACE_NAME, Constants.JOB_REQUEST)
        val config = """{"search":{"type":"local","queries":[{"file":"src/test/resources/data-exhaust/consumption-summ/*"}]},"model":"org.ekstep.analytics.model.DataExhaustJobModel","output":[{"to":"file","params":{"file": "/tmp/dataexhaust"}}],"parallelization":8,"appName":"Data Exhaust","deviceMapping":false, "modelParams":{}, "exhaustConfig":{"eks-consumption-summary":{"events":["ME_SESSION_SUMMARY"],"eventConfig":{"ME_SESSION_SUMMARY":{"eventType":"Summary","searchType":"local","fetchConfig":{"params":{"file":"src/test/resources/data-exhaust/consumption-summ/*"}},"filterMapping":{"tags":{"name":"genieTag","operator":"IN"}}}}}}}"""

        DataExhaustJob.main(config)(Option(sc));
    }

    it should "test for creation raw data" in {
        preProcess()

        val requests = Array(
            JobRequest("client-key2", "requestID2", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2017-06-22", "2017-06-22", None, Option(List("CP_IMPRESSION")), None, None), Option("eks-creation-raw"))),
                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None))

        sc.makeRDD(requests).saveToCassandra(Constants.PLATFORM_KEY_SPACE_NAME, Constants.JOB_REQUEST)
        val config = """{"search":{"type":"local","queries":[{"file":"src/test/resources/data-exhaust/creation-raw/*"}]},"model":"org.ekstep.analytics.model.DataExhaustJobModel","output":[{"to":"file","params":{"file": "/tmp/dataexhaust"}}],"parallelization":8,"appName":"Data Exhaust","deviceMapping":false, "modelParams":{}, "exhaustConfig":{"eks-creation-raw":{"events":["DEFAULT"],"eventConfig":{"DEFAULT":{"eventType":"CreationRaw","searchType":"local","fetchConfig":{"params":{"file":"src/test/resources/data-exhaust/creation-raw/*"}},"filterMapping":{"tags":{"name":"genieTag","operator":"IN"}}}}}}}"""

        DataExhaustJob.main(config)(Option(sc));
    }

    ignore should "run the data exhaust and save data to S3" in {

        preProcess()

        val requests = Array(
            JobRequest("partner1", "1234", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2016-09-01", "2016-09-10", Option(List("dff9175fa217e728d86bc1f4d8f818f6d2959303")), None, Option("appId"), Option("ChannelId")))),
                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None),
            JobRequest("partner1", "273645", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2016-11-19", "2016-11-20", Option(List("test-tag")), Option(List("OE_ASSESS")), Option("appId"), Option("ChannelId")))),
                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None));

        sc.makeRDD(requests).saveToCassandra(Constants.PLATFORM_KEY_SPACE_NAME, Constants.JOB_REQUEST)

        val config = """{"search":{"type":"s3"},"model":"org.ekstep.analytics.model.DataExhaustJobModel","modelParams":{"dataset-raw-bucket":"ekstep-datasets-test","consumption-raw-prefix":"staging/datasets/D001/4208ab995984d222b59299e5103d350a842d8d41/","data-exhaust-bucket":"ekstep-public","data-exhaust-prefix":"dev/data-exhaust"}, "parallelization":8,"appName":"Data Exhaust","deviceMapping":false}"""
        DataExhaustJob.main(config)(Option(sc));
    }

    ignore should "run the data exhaust for consumption summary data and save it to S3" in {

        preProcess()

        val requests = Array(
            JobRequest("client-key1", "requestID1", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2017-06-18", "2017-06-18", Option(List()), Option(List("ME_SESSION_SUMMARY")), Option("appId"), Option("ChannelId")), Option("D003"))),
                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None))

        sc.makeRDD(requests).saveToCassandra(Constants.PLATFORM_KEY_SPACE_NAME, Constants.JOB_REQUEST)

        val config = """{"search":{"type":"s3"},"model":"org.ekstep.analytics.model.DataExhaustJobModel","modelParams":{"dataset-raw-bucket":"ekstep-dev-data-store","consumption-raw-prefix":"ss/","data-exhaust-bucket":"ekstep-public-dev","data-exhaust-prefix":"data-exhaust/test","tempLocalPath":"/tmp/dataexhaust"}, "parallelization":8,"appName":"Data Exhaust","deviceMapping":false}"""
        DataExhaustJob.main(config)(Option(sc));
    }

    //    it should "run the data exhaust for consumption metrics data and save it to S3" in {
    //
    //        preProcess()
    //
    //        val requests = Array(
    //            JobRequest("client-key1", "requestID1", None, "SUBMITTED", JSONUtils.serialize(RequestConfig(RequestFilter("2017-06-22", "2017-06-22", Option(List()), Option(List("ME_CONTENT_USAGE_METRICS")),Option("appId"), Option("ChannelId")), Option("D004"))),
    //                None, None, None, None, None, None, DateTime.now(), None, None, None, None, None, None, None, None, None, None))
    //
    //        sc.makeRDD(requests).saveToCassandra(Constants.PLATFORM_KEY_SPACE_NAME, Constants.JOB_REQUEST)
    //
    //        val config = """{"search":{"type":"s3"},"model":"org.ekstep.analytics.model.DataExhaustJobModel","modelParams":{"dataset-raw-bucket":"ekstep-data-sets-dev","bucket":"ekstep-dev-data-store","consumption-raw-prefix":"datasets/D001/4208ab995984d222b59299e5103d350a842d8d41/","creation-raw-prefix":"portal/","data-exhaust-bucket":"ekstep-public-dev","data-exhaust-prefix":"data-exhaust/test","tempLocalPath":"/tmp/dataexhaust"}, "parallelization":8,"appName":"Data Exhaust","deviceMapping":false}"""
    //        DataExhaustJob.main(config)(Option(sc));
    //
    //        val fileDetails = Map("fileType" -> "s3", "bucket" -> "ekstep-public-dev", "prefix" -> "data-exhaust/test/")
    //        val request_ids = Array("requestID1")
    //        postProcess(fileDetails, request_ids)
    //    }
}
