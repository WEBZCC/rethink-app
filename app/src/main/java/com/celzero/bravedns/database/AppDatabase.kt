/*
Copyright 2020 RethinkDNS developers

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.celzero.bravedns.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppInfo::class, ConnectionTracker::class, CustomIp::class, DoHEndpoint::class, DnsCryptEndpoint::class, DnsProxyEndpoint::class, DnsCryptRelayEndpoint::class, ProxyEndpoint::class, DnsLog::class, CustomDomain::class, RethinkDnsEndpoint::class],
    version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private const val DATABASE_NAME = "bravedns.db"
        private const val DATABASE_PATH = "database/rethink_v12.db"

        // setJournalMode() is added as part of issue #344
        fun buildDatabase(context: Context) = Room.databaseBuilder(context.applicationContext,
                                                                   AppDatabase::class.java,
                                                                   DATABASE_NAME)
            .createFromAsset(DATABASE_PATH)
            .setJournalMode(JournalMode.TRUNCATE).addMigrations(MIGRATION_1_2).addMigrations(
                MIGRATION_2_3).addMigrations(MIGRATION_3_4).addMigrations(
                MIGRATION_4_5).addMigrations(MIGRATION_5_6).addMigrations(
                MIGRATION_6_7).addMigrations(MIGRATION_7_8).addMigrations(
                MIGRATION_8_9).addMigrations(MIGRATION_9_10).addMigrations(
                MIGRATION_10_11).addMigrations(MIGRATION_11_12).build()

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE from AppInfo")
                database.execSQL("DELETE from CategoryInfo")
                database.execSQL(
                    "CREATE TABLE 'CategoryInfo' ( 'categoryName' TEXT NOT NULL, 'numberOFApps' INTEGER NOT NULL,'numOfAppsBlocked' INTEGER NOT NULL, 'isInternetBlocked' INTEGER NOT NULL, PRIMARY KEY (categoryName)) ")
            }
        }


        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE from AppInfo ")
                database.execSQL("DELETE from CategoryInfo")
                database.execSQL("DROP TABLE if exists ConnectionTracker")
                database.execSQL(
                    "CREATE TABLE 'ConnectionTracker' ('id' INTEGER NOT NULL,'appName' TEXT, 'uid' INTEGER NOT NULL, 'ipAddress' TEXT, 'port' INTEGER NOT NULL, 'protocol' INTEGER NOT NULL,'isBlocked' INTEGER NOT NULL, 'flag' TEXT, 'timeStamp' INTEGER NOT NULL,PRIMARY KEY (id)  )")
                database.execSQL(
                    "CREATE TABLE 'BlockedConnections' ( 'id' INTEGER NOT NULL, 'uid' INTEGER NOT NULL, 'ipAddress' TEXT, 'port' INTEGER NOT NULL, 'protocol' TEXT, PRIMARY KEY (id)) ")
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE BlockedConnections ADD COLUMN isActive INTEGER DEFAULT 1 NOT NULL")
                database.execSQL(
                    "ALTER TABLE BlockedConnections ADD COLUMN ruleType TEXT DEFAULT 'RULE4' NOT NULL")
                database.execSQL(
                    "ALTER TABLE BlockedConnections ADD COLUMN modifiedDateTime INTEGER DEFAULT 0  NOT NULL")
                database.execSQL(
                    "UPDATE BlockedConnections set ruleType = 'RULE5' where uid = -1000")
                database.execSQL("ALTER TABLE ConnectionTracker ADD COLUMN blockedByRule TEXT")
                database.execSQL(
                    "UPDATE ConnectionTracker set blockedByRule = 'RULE4' where uid <> -1000 and isBlocked = 1")
                database.execSQL(
                    "UPDATE ConnectionTracker set blockedByRule = 'RULE5' where uid = -1000  and isBlocked = 1")
                database.execSQL(
                    "ALTER TABLE AppInfo add column whiteListUniv1 INTEGER DEFAULT 0 NOT NULL")
                database.execSQL(
                    "ALTER TABLE AppInfo add column whiteListUniv2 INTEGER DEFAULT 0 NOT NULL")
                database.execSQL(
                    "ALTER TABLE AppInfo add column isExcluded INTEGER DEFAULT 0 NOT NULL")
                database.execSQL(
                    "CREATE TABLE 'DoHEndpoint' ( 'id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'dohName' TEXT NOT NULL, 'dohURL' TEXT NOT NULL,'dohExplanation' TEXT, 'isSelected' INTEGER NOT NULL, 'isCustom' INTEGER NOT NULL,'modifiedDataTime' INTEGER NOT NULL, 'latency' INTEGER NOT NULL) ")
                database.execSQL(
                    "CREATE TABLE 'DNSCryptEndpoint' ( 'id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'dnsCryptName' TEXT NOT NULL, 'dnsCryptURL' TEXT NOT NULL,'dnsCryptExplanation' TEXT, 'isSelected' INTEGER NOT NULL, 'isCustom' INTEGER NOT NULL,'modifiedDataTime' INTEGER NOT NULL, 'latency' INTEGER NOT NULL) ")
                database.execSQL(
                    "CREATE TABLE 'DNSCryptRelayEndpoint' ( 'id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'dnsCryptRelayName' TEXT NOT NULL, 'dnsCryptRelayURL' TEXT NOT NULL,'dnsCryptRelayExplanation' TEXT, 'isSelected' INTEGER NOT NULL, 'isCustom' INTEGER NOT NULL,'modifiedDataTime' INTEGER NOT NULL, 'latency' INTEGER NOT NULL) ")
                database.execSQL(
                    "CREATE TABLE 'DNSProxyEndpoint' ( 'id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'proxyName' TEXT NOT NULL, 'proxyType' TEXT NOT NULL,'proxyAppName' TEXT , 'proxyIP' TEXT, 'proxyPort' INTEGER NOT NULL, 'isSelected' INTEGER NOT NULL, 'isCustom' INTEGER NOT NULL,'modifiedDataTime' INTEGER NOT NULL, 'latency' INTEGER NOT NULL) ")
                database.execSQL(
                    "CREATE TABLE 'ProxyEndpoint' ( 'id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'proxyName' TEXT NOT NULL,'proxyMode' INTEGER NOT NULL, 'proxyType' TEXT NOT NULL,'proxyAppName' TEXT , 'proxyIP' TEXT, 'userName' TEXT , 'password' TEXT, 'proxyPort' INTEGER NOT NULL, 'isSelected' INTEGER NOT NULL, 'isCustom' INTEGER NOT NULL , 'isUDP' INTEGER NOT NULL,'modifiedDataTime' INTEGER NOT NULL, 'latency' INTEGER NOT NULL) ")
                //Perform insert of endpoints
                database.execSQL(
                    "INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(1,'Cloudflare','https://cloudflare-dns.com/dns-query','Does not block any DNS requests. Uses Cloudflare''s 1.1.1.1 DNS endpoint.',0,0,0,0)")
                database.execSQL(
                    "INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(2,'Cloudflare Family','https://family.cloudflare-dns.com/dns-query','Blocks malware and adult content. Uses Cloudflare''s 1.1.1.3 DNS endpoint.',0,0,0,0)")
                database.execSQL(
                    "INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(3,'Cloudflare Security','https://security.cloudflare-dns.com/dns-query','Blocks malicious content. Uses Cloudflare''s 1.1.1.2 DNS endpoint.',0,0,0,0)")
                database.execSQL(
                    "INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(4,'RethinkDNS Basic (default)','https://basic.bravedns.com/1:YBcgAIAQIAAIAABgIAA=','Blocks malware and more. Uses RethinkDNS''s non-configurable basic endpoint.',1,0,0,0)")
                database.execSQL(
                    "INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(5,'RethinkDNS Plus','https://basic.bravedns.com/','Configurable DNS endpoint: Provides in-depth analytics of your Internet traffic, allows you to set custom rules and more.',0,0,0,0)")
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE from DNSProxyEndpoint")
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL  = 'https://basic.bravedns.com/1:wBdgAIoBoB02kIAA5HI=' where id = 4")
                database.execSQL(
                    "UPDATE DNSCryptEndpoint set dnsCryptName='Quad9', dnsCryptURL='sdns://AQYAAAAAAAAAEzE0OS4xMTIuMTEyLjEwOjg0NDMgZ8hHuMh1jNEgJFVDvnVnRt803x2EwAuMRwNo34Idhj4ZMi5kbnNjcnlwdC1jZXJ0LnF1YWQ5Lm5ldA',dnsCryptExplanation='Quad9 (anycast) no-dnssec/no-log/no-filter 9.9.9.10 / 149.112.112.10' where id=5")
                database.execSQL(
                    "INSERT into DNSProxyEndpoint values (1,'Google','External','Nobody','8.8.8.8',53,0,0,0,0)")
                database.execSQL(
                    "INSERT into DNSProxyEndpoint values (2,'Cloudflare','External','Nobody','1.1.1.1',53,0,0,0,0)")
                database.execSQL(
                    "INSERT into DNSProxyEndpoint values (3,'Quad9','External','Nobody','9.9.9.9',53,0,0,0,0)")
                database.execSQL(
                    "UPDATE DNSCryptEndpoint set dnsCryptName ='Cleanbrowsing Family' where id = 1")
                database.execSQL("UPDATE DNSCryptEndpoint set dnsCryptName ='Adguard' where id = 2")
                database.execSQL(
                    "UPDATE DNSCryptEndpoint set dnsCryptName ='Adguard Family' where id = 3")
                database.execSQL(
                    "UPDATE DNSCryptEndpoint set dnsCryptName ='Cleanbrowsing Security' where id = 4")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Anon-AMS-NL' where id = 1")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Anon-CS-FR' where id = 2")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Anon-CS-SE' where id = 3")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Anon-CS-USCA' where id = 4")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Anon-Tiarap' where id = 5")
            }
        }

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE 'DNSLogs' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'queryStr' TEXT NOT NULL, 'time' INTEGER NOT NULL, 'flag' TEXT NOT NULL, 'resolver' TEXT NOT NULL, 'latency' INTEGER NOT NULL, 'typeName' TEXT NOT NULL, 'isBlocked' INTEGER NOT NULL, 'blockLists' LONGTEXT NOT NULL,  'serverIP' TEXT NOT NULL, 'relayIP' TEXT NOT NULL, 'responseTime' INTEGER NOT NULL, 'response' TEXT NOT NULL, 'status' TEXT NOT NULL,'dnsType' INTEGER NOT NULL) ")
                //https://basic.bravedns.com/1:YBIgACABAHAgAA== - New block list configured
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL  = 'https://basic.bravedns.com/1:YBcgAIAQIAAIAABgIAA=' where id = 4")
                database.execSQL(
                    "UPDATE DNSCryptEndpoint set dnsCryptName='Quad9', dnsCryptURL='sdns://AQMAAAAAAAAADDkuOS45Ljk6ODQ0MyBnyEe4yHWM0SAkVUO-dWdG3zTfHYTAC4xHA2jfgh2GPhkyLmRuc2NyeXB0LWNlcnQucXVhZDkubmV0',dnsCryptExplanation='Quad9 (anycast) dnssec/no-log/filter 9.9.9.9 / 149.112.112.9' where id=5")
                database.execSQL(
                    "ALTER TABLE CategoryInfo add column numOfAppWhitelisted INTEGER DEFAULT 0 NOT NULL")
                database.execSQL(
                    "ALTER TABLE CategoryInfo add column numOfAppsExcluded INTEGER DEFAULT 0 NOT NULL")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Netherlands' where id = 1")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='France' where id = 2")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Sweden' where id = 3")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='US - Los Angeles, CA' where id = 4")
                database.execSQL(
                    "UPDATE DNSCryptRelayEndpoint set dnsCryptRelayName ='Singapore' where id = 5")
            }
        }

        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL  = 'https://security.cloudflare-dns.com/dns-query' where id = 3")
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL  = 'https://basic.bravedns.com/1:YBcgAIAQIAAIAABgIAA=' where id = 4")
            }
        }

        /**
         * For the version 053-1. Created a view for the AppInfo table so that the read will be minimized.
         * Also deleting the uid=0 row from AppInfo table. In earlier version the UID=0 is added as default and
         * not used. Now the UID=0(ANDROID) is added to the non-app category.
         */
        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE VIEW `AppInfoView` AS select appName, appCategory, isInternetAllowed, whiteListUniv1, isExcluded from AppInfo")
                database.execSQL(
                    "UPDATE AppInfo set appCategory = 'System Components' where uid = 0")
                database.execSQL(
                    "DELETE from AppInfo where appName = 'ANDROID' and appCategory = 'System Components'")
            }
        }

        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL  = 'https://basic.bravedns.com/1:YASAAQBwIAA=' where id = 4")
            }
        }

        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL  = 'https://basic.bravedns.com/1:IAAgAA==' where id = 4")
            }
        }

        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE DNSLogs add column responseIps TEXT DEFAULT '' NOT NULL")
                database.execSQL(
                    "CREATE TABLE 'CustomDomain' ( 'domain' TEXT NOT NULL, 'ips' TEXT NOT NULL, 'status' INTEGER NOT NULL, 'createdTs' DATE NOT NULL, 'deletedTs' DATE NOT NULL, 'version' INTEGER NOT NULL, PRIMARY KEY (domain)) ")
            }
        }

        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                removeRethinkFromDohList(database)
                addMoreDohToList(database)
                modifyAppInfoTableSchema(database)
                modifyBlockedConnectionsTable(database)
                addNetworkDns(database)
                database.execSQL("DROP VIEW AppInfoView")
                database.execSQL("DROP TABLE if exists CategoryInfo")
                database.execSQL(
                    "UPDATE DoHEndpoint set dohURL = `replace`(dohURL,'bravedns','rethinkdns')")
                database.execSQL(
                    "ALTER TABLE CustomDomain add column wildcard INTEGER DEFAULT 0 NOT NULL")
                modifyConnectionTrackerTable(database)
                createRethinkDnsTable(database)
            }

            // remove the rethink doh from the list
            private fun removeRethinkFromDohList(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL("DELETE from DoHEndpoint where id in (4,5)")
                }
            }

            // add more doh options as default
            private fun addMoreDohToList(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL("INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(4,'Google','https://dns.google/dns-query','Traditional DNS queries and replies are sent over UDP or TCP without encryption, making them subject to surveillance, spoofing, and DNS-based Internet filtering.',0,0,0,0)")
                    execSQL("INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(5,'CleanBrowsing Family','https://doh.cleanbrowsing.org/doh/family-filter/','Family filter blocks access to all adult, pornographic and explicit sites. It also blocks proxy and VPN domains that could be used to bypass our filters. Mixed content sites (like Reddit) are also blocked. Google, Bing and Youtube are set to the Safe Mode.',0,0,0,0)")
                    execSQL("INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(6,'CleanBrowsing Adult','https://doh.cleanbrowsing.org/doh/adult-filter/','Adult filter blocks access to all adult, pornographic and explicit sites. It does not block proxy or VPNs, nor mixed-content sites. Sites like Reddit are allowed. Google and Bing are set to the Safe Mode.',0,0,0,0)")
                    execSQL("INSERT OR REPLACE INTO DoHEndpoint(id,dohName,dohURL,dohExplanation, isSelected,isCustom,modifiedDataTime,latency) values(7,'Quad9 Secure','https://dns.quad9.net/dns-query','Quad9 routes your DNS queries through a secure network of servers around the globe.',0,0,0,0)")
                }
            }

            // add network dns option in dns proxy
            private fun addNetworkDns(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL("UPDATE DNSProxyEndpoint set id = 4 where id = 3")
                    execSQL("UPDATE DNSProxyEndpoint set id = 3 where id = 2")
                    execSQL("UPDATE DNSProxyEndpoint set id = 2 where id = 1")
                    execSQL(
                        "INSERT INTO DNSProxyEndpoint values (1,'Network DNS','External','Nobody','',53,0,0,0,0)")
                }
            }

            // rename blockedConnections table to CustomIp
            private fun modifyBlockedConnectionsTable(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL(
                        "CREATE TABLE 'CustomIp' ('uid' INTEGER NOT NULL, 'ipAddress' TEXT DEFAULT '' NOT NULL, 'port' INTEGER DEFAULT '' NOT NULL, 'protocol' TEXT DEFAULT '' NOT NULL, 'isActive' INTEGER DEFAULT 1 NOT NULL, 'status' INTEGER DEFAULT 1 NOT NULL,'ruleType' INTEGER DEFAULT 0 NOT NULL, 'wildcard' INTEGER DEFAULT 0 NOT NULL, 'modifiedDateTime' INTEGER DEFAULT 0 NOT NULL, PRIMARY KEY(uid, ipAddress, port, protocol))")
                    execSQL(
                        "INSERT INTO 'CustomIp' SELECT uid, ipAddress, port, protocol, isActive, 1, 0, 0, modifiedDateTime from BlockedConnections")
                    execSQL("DROP TABLE if exists BlockedConnections")
                }
            }

            private fun modifyAppInfoTableSchema(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL(
                        "CREATE TABLE 'AppInfo_backup' ('packageInfo' TEXT PRIMARY KEY NOT NULL, 'appName' TEXT NOT NULL, 'uid' INTEGER NOT NULL, 'isSystemApp' INTEGER NOT NULL, 'firewallStatus' INTEGER NOT NULL DEFAULT 0, 'appCategory' TEXT NOT NULL, 'wifiDataUsed' INTEGER NOT NULL, 'mobileDataUsed' INTEGER NOT NULL, 'metered' INTEGER NOT NULL DEFAULT 0, 'screenOffAllowed' INTEGER NOT NULL DEFAULT 0, 'backgroundAllowed' INTEGER NOT NULL DEFAULT 0,  'isInternetAllowed' INTEGER NOT NULL, 'whiteListUniv1' INTEGER NOT NULL, 'isExcluded' INTEGER NOT NULL)")
                    execSQL(
                        "INSERT INTO AppInfo_backup SELECT packageInfo, appName, uid, isSystemApp, 0, appCategory, wifiDataUsed, mobileDataUsed, 0, isScreenOff, isBackgroundEnabled, isInternetAllowed, whiteListUniv1, isExcluded FROM AppInfo")
                    execSQL(
                        "UPDATE AppInfo_backup set firewallStatus = 2 where isInternetAllowed = 1")
                    execSQL("UPDATE AppInfo_backup set firewallStatus = 3 where whiteListUniv1 = 1")
                    execSQL("UPDATE AppInfo_backup set firewallStatus = 4 where isExcluded = 1")
                    execSQL(" DROP TABLE if exists AppInfo")
                    execSQL(
                        "CREATE TABLE 'AppInfo' ('packageInfo' TEXT PRIMARY KEY NOT NULL, 'appName' TEXT NOT NULL, 'uid' INTEGER NOT NULL, 'isSystemApp' INTEGER NOT NULL, 'firewallStatus' INTEGER NOT NULL DEFAULT 0, 'appCategory' TEXT NOT NULL, 'wifiDataUsed' INTEGER NOT NULL, 'mobileDataUsed' INTEGER NOT NULL, 'metered' INTEGER NOT NULL DEFAULT 0, 'screenOffAllowed' INTEGER NOT NULL DEFAULT 0, 'backgroundAllowed' INTEGER NOT NULL DEFAULT 0)")
                    execSQL(
                        "INSERT INTO AppInfo SELECT packageInfo, appName, uid, isSystemApp, firewallStatus, appCategory, wifiDataUsed, mobileDataUsed, metered, screenOffAllowed, backgroundAllowed FROM AppInfo_backup")

                    execSQL("DROP TABLE AppInfo_backup")
                }
            }

            // To introduce NOT NULL property for columns in the schema, alter table query cannot
            // add the not-null to the schema, so creating a backup and recreating the table
            // during migration.
            private fun modifyConnectionTrackerTable(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL(
                        "CREATE TABLE 'ConnectionTracker_backup' ('id' INTEGER NOT NULL,'appName' TEXT DEFAULT '' NOT NULL, 'uid' INTEGER NOT NULL, 'ipAddress' TEXT DEFAULT ''  NOT NULL, 'port' INTEGER NOT NULL, 'protocol' INTEGER NOT NULL,'isBlocked' INTEGER NOT NULL, 'blockedByRule' TEXT DEFAULT '' NOT NULL, 'flag' TEXT  DEFAULT '' NOT NULL, 'dnsQuery' TEXT DEFAULT '', 'timeStamp' INTEGER NOT NULL,PRIMARY KEY (id)  )")
                    execSQL(
                        "INSERT INTO ConnectionTracker_backup SELECT id, appName, uid, ipAddress, port, protocol, isBlocked, blockedByRule, flag, '', timeStamp from ConnectionTracker")
                    execSQL("DROP TABLE if exists ConnectionTracker")
                    execSQL(
                        "CREATE TABLE 'ConnectionTracker' ('id' INTEGER NOT NULL,'appName' TEXT DEFAULT '' NOT NULL, 'uid' INTEGER NOT NULL, 'ipAddress' TEXT DEFAULT ''  NOT NULL, 'port' INTEGER NOT NULL, 'protocol' INTEGER NOT NULL,'isBlocked' INTEGER NOT NULL, 'blockedByRule' TEXT DEFAULT '' NOT NULL, 'flag' TEXT  DEFAULT '' NOT NULL, 'dnsQuery' TEXT DEFAULT '', 'timeStamp' INTEGER NOT NULL,PRIMARY KEY (id)  )")
                    execSQL(
                        "INSERT INTO ConnectionTracker SELECT id, appName, uid, ipAddress, port, protocol, isBlocked, blockedByRule, flag, '',  timeStamp from ConnectionTracker_backup")
                    execSQL("DROP TABLE if exists ConnectionTracker_backup")
                }
            }

            // create new table to store Rethink dns endpoint
            // contains both the global and app specific dns endpoints
            private fun createRethinkDnsTable(database: SupportSQLiteDatabase) {
                with(database) {
                    execSQL(
                        "CREATE TABLE 'RethinkDnsEndpoint' ('name' TEXT NOT NULL, 'url' TEXT NOT NULL, 'uid' INTEGER NOT NULL, 'desc' TEXT NOT NULL, 'isActive' INTEGER NOT NULL, 'isCustom' INTEGER NOT NULL, 'latency' INTEGER NOT NULL, 'modifiedDataTime' INTEGER NOT NULL, PRIMARY KEY (name, uid))")
                }
            }
        }

    }

    abstract fun appInfoDAO(): AppInfoDAO
    abstract fun connectionTrackerDAO(): ConnectionTrackerDAO
    abstract fun dohEndpointsDAO(): DoHEndpointDAO
    abstract fun dnsCryptEndpointDAO(): DnsCryptEndpointDAO
    abstract fun dnsCryptRelayEndpointDAO(): DnsCryptRelayEndpointDAO
    abstract fun dnsProxyEndpointDAO(): DnsProxyEndpointDAO
    abstract fun proxyEndpointDAO(): ProxyEndpointDAO
    abstract fun dnsLogDAO(): DnsLogDAO
    abstract fun customDomainEndpointDAO(): CustomDomainDAO
    abstract fun customIpEndpointDao(): CustomIpDao
    abstract fun rethinkEndpointDao(): RethinkDnsEndpointDao

    fun appInfoRepository() = AppInfoRepository(appInfoDAO())
    fun connectionTrackerRepository() = ConnectionTrackerRepository(connectionTrackerDAO())
    fun dohEndpointRepository() = DoHEndpointRepository(dohEndpointsDAO())
    fun dnsCryptEndpointRepository() = DnsCryptEndpointRepository(dnsCryptEndpointDAO())
    fun dnsCryptRelayEndpointRepository() = DnsCryptRelayEndpointRepository(
        dnsCryptRelayEndpointDAO())

    fun dnsProxyEndpointRepository() = DnsProxyEndpointRepository(dnsProxyEndpointDAO())
    fun proxyEndpointRepository() = ProxyEndpointRepository(proxyEndpointDAO())
    fun dnsLogRepository() = DnsLogRepository(dnsLogDAO())
    fun customDomainRepository() = CustomDomainRepository(customDomainEndpointDAO())
    fun customIpRepository() = CustomIpRepository(customIpEndpointDao())
    fun rethinkEndpointRepository() = RethinkDnsEndpointRepository(rethinkEndpointDao())
}
