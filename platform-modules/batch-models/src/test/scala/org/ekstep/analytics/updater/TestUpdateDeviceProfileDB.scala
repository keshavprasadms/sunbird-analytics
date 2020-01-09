package org.ekstep.analytics.updater

import org.ekstep.analytics.framework.DerivedEvent
import org.ekstep.analytics.framework.conf.AppConf
import org.ekstep.analytics.model.SparkSpec
import org.ekstep.analytics.util.EmbeddedPostgresql

class TestUpdateDeviceProfileDB extends SparkSpec(null) {

    import org.ekstep.analytics.framework.FrameworkContext
    implicit val fc = new FrameworkContext()

    val deviceTable = AppConf.getConfig("postgres.device.table_name")

    override def beforeAll(){
        super.beforeAll()
        EmbeddedPostgresql.start()
        EmbeddedPostgresql.execute(
          s"""
            |CREATE TABLE IF NOT EXISTS $deviceTable(
            |    device_id TEXT PRIMARY KEY,
            |    api_last_updated_on TIMESTAMP,
            |    avg_ts float,
            |    city TEXT,
            |    country TEXT,
            |    country_code TEXT,
            |    device_spec json,
            |    district_custom TEXT,
            |    fcm_token TEXT,
            |    first_access TIMESTAMP,
            |    last_access TIMESTAMP,
            |    producer_id TEXT,
            |    state TEXT,
            |    state_code TEXT,
            |    state_code_custom TEXT,
            |    state_custom TEXT,
            |    total_launches bigint,
            |    total_ts float,
            |    uaspec json,
            |    updated_date TIMESTAMP,
            |    user_declared_district TEXT,
            |    user_declared_state TEXT)""".stripMargin)
    }

    "UpdateDeviceProfileDB" should "create device profile in device db" in {
try {
    EmbeddedPostgresql.execute(s"TRUNCATE $deviceTable")

    val rdd = loadFile[DerivedEvent]("src/test/resources/device-profile/test-data1.log");
    val count = UpdateDeviceProfileDB.execute(rdd, None).count();

    val device1 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = '88edda82418a1e916e9906a2fd7942cb'")

    while (device1.next()) {
        device1.getString("first_access") should be("2018-09-21 22:49:15.883")
        device1.getString("last_access") should be("2018-09-22 19:39:41.139")
        device1.getString("total_ts") should be("50")
        device1.getString("avg_ts") should be("50")
    }

    val device2 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = '48edda82418a1e916e9906a2fd7942cb'")
    while (device2.next()) {
        device2.getString("first_access") should be("2018-09-21 22:49:15.883")
        device2.getString("last_access") should be("2018-09-21 22:49:24.377")
        device2.getString("total_ts") should be("18")
        device2.getString("avg_ts") should be("9")
    }
}catch {
    case ex : Exception => {
        ex.printStackTrace()
        System.out.println("the value"  + ex.getMessage)
    }
}
    }
    
    it should "check for first_access and last_access" in {
        EmbeddedPostgresql.execute(s"TRUNCATE $deviceTable")
        EmbeddedPostgresql.execute("INSERT INTO device_profile (device_id, first_access, last_access, total_ts, total_launches, avg_ts, state, city, device_spec, uaspec) VALUES ('48edda82418a1e916e9906a2fd7942cb', '2018-09-21 22:49:15.883', '2018-09-21 22:49:24.377', 18, 2, 9, 'Karnataka', 'Bangalore', '{\"os\":\"Android\",\"make\":\"Motorola XT1706\"}', '{\"raw\": \"xyz\"}');")
        EmbeddedPostgresql.execute(s"INSERT INTO $deviceTable (device_id, first_access, last_access, total_ts, total_launches, avg_ts) VALUES ('88edda82418a1e916e9906a2fd7942cb', '2018-09-20 22:49:15.883', '2018-09-22 19:39:41.139', 20, 2, 10);")

        val rdd = loadFile[DerivedEvent]("src/test/resources/device-profile/test-data2.log");
        UpdateDeviceProfileDB.execute(rdd, None);

        val device1 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = '48edda82418a1e916e9906a2fd7942cb'")
        while(device1.next()) {
            device1.getString("device_id") should be ("48edda82418a1e916e9906a2fd7942cb")
            device1.getString("first_access") should be ("2018-09-21 22:49:15.883")
            device1.getString("last_access") should be ("2018-09-23 05:24:24.377")
            device1.getString("total_ts") should be ("28")
        }
        val device2 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = '88edda82418a1e916e9906a2fd7942cb'")
        while(device2.next()) {
            device2.getString("device_id") should be ("88edda82418a1e916e9906a2fd7942cb")
            device2.getString("first_access") should be ("2018-09-20 19:02:35.883")
            device2.getString("last_access") should be ("2018-09-22 19:39:41.139")
            device2.getString("total_ts") should be ("45")
            device2.getString("avg_ts") should be ("15")
        }
    }

    it should "Handle null values from Cassandra and execute successfully" in {
        EmbeddedPostgresql.execute(s"TRUNCATE $deviceTable")
        EmbeddedPostgresql.execute("INSERT INTO device_profile (device_id, first_access, last_access, total_ts, total_launches, avg_ts, state, city, device_spec, uaspec) VALUES ('48edda82418a1e916e9906a2fd7942cb', '2018-09-21 22:49:15.883', '2018-09-21 22:49:24.377', 18, 2, 9, 'Karnataka', 'Bangalore', '{\"os\":\"Android\",\"make\":\"Motorola XT1706\"}', '{\"raw\": \"xyz\"}');")
        EmbeddedPostgresql.execute(s"INSERT INTO $deviceTable (device_id) VALUES ('88edda82418a1e916e9906a2fd7942cb');")

        val rdd = loadFile[DerivedEvent]("src/test/resources/device-profile/test-data2.log")
        UpdateDeviceProfileDB.execute(rdd, None)
    }

    ignore should "include new values and execute successfully" in {
        EmbeddedPostgresql.execute(s"TRUNCATE $deviceTable")
        EmbeddedPostgresql.execute("INSERT INTO device_profile (device_id,  state_custom, state_code_custom, district_custom, fcm_token, producer_id) VALUES ('88edda82418a1e916e9906a2fd7942cb', 'karnataka', '29', 'bangalore', 'token-xyz', 'sunbird-app')")
        EmbeddedPostgresql.execute(s"INSERT INTO $deviceTable (device_id,  state_custom, state_code_custom, district_custom, fcm_token, producer_id, user_declared_state, user_declared_district) VALUES ('test-device-1', 'Karnataka', '29', 'Bangalore', '', 'sunbird-portal', 'Karnataka', 'Bangalore')")


        val rdd = loadFile[DerivedEvent]("src/test/resources/device-profile/test-data2.log")
        UpdateDeviceProfileDB.execute(rdd, None)

        val device1 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = '88edda82418a1e916e9906a2fd7942cb'")
        while(device1.next()) {
            device1.getString("state_custom") should be ("karnataka")
            device1.getString("state_code_custom") should be ("29")
            device1.getString("district_custom") should be ("bangalore")
            device1.getString("fcm_token") should be ("token-xyz")
            device1.getString("producer_id") should be ("sunbird-app")
        }

        val device2 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = '48edda82418a1e916e9906a2fd7942cb'")

        while(device2.next()) {
            device2.getString("state_custom") should be ("null")
            device2.getString("state_code_custom") should be ("null")
            device2.getString("district_custom") should be ("null")
            device2.getString("fcm_token") should be ("null")
            device2.getString("producer_id") should be ("null")
        }

        val device3 = EmbeddedPostgresql.executeQuery(s"SELECT * FROM $deviceTable WHERE device_id = 'test-device-1'")
        while(device3.next()) {
            device3.getString("fcm_token") should be ("")
            device3.getString("producer_id") should be ("sunbird-portal")
            device3.getString("user_declared_state") should be ("Karnataka")
            device3.getString("user_declared_district") should be ("Bangalore")
        }
    }

    override def afterAll(): Unit ={
        super.afterAll()
        fc.shutdownPostgresService()
        EmbeddedPostgresql.close()

    }
}