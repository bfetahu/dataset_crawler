CREATE DATABASE  IF NOT EXISTS `ld_dataset_crawler` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `ld_dataset_crawler`;
-- MySQL dump 10.13  Distrib 5.6.13, for Win32 (x86)
--
-- Host: db.l3s.uni-hannover.de    Database: ld_dataset_crawler
-- ------------------------------------------------------
-- Server version	5.0.95

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Not dumping tablespaces as no INFORMATION_SCHEMA.FILES table on this server
--

--
-- Table structure for table `crawl_log`
--

DROP TABLE IF EXISTS `crawl_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `crawl_log` (
  `crawl_id` int(11) NOT NULL auto_increment,
  `crawl_description` text,
  `crawl_date` timestamp NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`crawl_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `crawl_operations_log`
--

DROP TABLE IF EXISTS `crawl_operations_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `crawl_operations_log` (
  `log_id` int(11) NOT NULL auto_increment,
  `log_type` varchar(500) NOT NULL default '',
  `log_description` text NOT NULL,
  `log_date` datetime NOT NULL default '0000-00-00 00:00:00',
  `log_method` varchar(500) NOT NULL default '',
  `crawl_id` int(11) default NULL,
  PRIMARY KEY  (`log_id`),
  UNIQUE KEY `log_id_UNIQUE` (`log_id`),
  KEY `fk_crawl_idx` (`crawl_id`),
  KEY `log_type_idx` (`log_type`(255)),
  KEY `log_date_idx` (`log_date`),
  KEY `log_method_idx` (`log_method`(255)),
  KEY `crawl_idx` (`crawl_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2047875 DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 7168 kB';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `crawl_setups`
--

DROP TABLE IF EXISTS `crawl_setups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `crawl_setups` (
  `setup_id` int(10) unsigned NOT NULL auto_increment,
  `datahub_keywords` text NOT NULL,
  `crawl_description` varchar(45) default NULL,
  `is_completed` tinyint(1) default NULL,
  `user_id` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`setup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='InnoDB free: 8192 kB; InnoDB free: 4096 kB; InnoDB free: 112';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset`
--

DROP TABLE IF EXISTS `dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset` (
  `dataset_id` int(11) NOT NULL auto_increment,
  `dataset_name` varchar(500) NOT NULL,
  `dataset_description` varchar(500) default NULL,
  `dataset_url` varchar(500) default NULL,
  `dataset_id_datahub` varchar(200) NOT NULL,
  PRIMARY KEY  (`dataset_id`),
  UNIQUE KEY `dataset_id_datahub_UNIQUE` (`dataset_id_datahub`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_availability`
--

DROP TABLE IF EXISTS `dataset_availability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_availability` (
  `crawl_id` int(11) NOT NULL,
  `dataset_id` int(11) NOT NULL,
  `is_available` tinyint(1) NOT NULL,
  PRIMARY KEY  (`crawl_id`,`dataset_id`),
  KEY `fk_crawl_idx` (`crawl_id`),
  KEY `fk_dataset_idx` (`dataset_id`),
  CONSTRAINT `fk_crawl` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_dataset` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`dataset_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_crawl_log`
--

DROP TABLE IF EXISTS `dataset_crawl_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_crawl_log` (
  `crawl_id` int(11) NOT NULL auto_increment,
  `dataset_id` int(11) NOT NULL default '0',
  `timestamp_start` timestamp NOT NULL default '0000-00-00 00:00:00',
  `timestamp_end` timestamp NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`crawl_id`,`dataset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 9216 kB; InnoDB free: 9216 kB';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_namespaces`
--

DROP TABLE IF EXISTS `dataset_namespaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_namespaces` (
  `dataset_id` int(11) NOT NULL,
  `namespace_id` int(11) NOT NULL,
  KEY `fk_dataset_id_idx` (`dataset_id`),
  KEY `fk_schema_id_idx` (`namespace_id`),
  CONSTRAINT `fk_schema_dataset_schemas` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_dataset_dataset_schemas` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`dataset_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_namespaces_log`
--

DROP TABLE IF EXISTS `dataset_namespaces_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dataset_namespaces_log` (
  `dataset_id` int(11) NOT NULL default '0',
  `namespace_id` int(11) NOT NULL,
  `log_type` varchar(45) default NULL,
  `crawl_id` int(11) default NULL,
  PRIMARY KEY  (`dataset_id`,`namespace_id`),
  KEY `fk_dataset_idx` (`dataset_id`),
  KEY `fk_schema_idx` (`namespace_id`),
  KEY `fk_crawl_idx` (`crawl_id`),
  KEY `log_type_idx` (`log_type`),
  KEY `crawl_idx` (`crawl_id`),
  CONSTRAINT `fk_schema_dataset_schema_log` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_crawl_dataset_schema_log` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_dataset_dataset_schema_log` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`dataset_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='InnoDB free: 21504 kB; (`crawl_id`) REFER `ld_dataset_crawle';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `namespace`
--

DROP TABLE IF EXISTS `namespace`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `namespace` (
  `namespace_id` int(11) NOT NULL auto_increment,
  `namespace_uri` varchar(200) NOT NULL,
  PRIMARY KEY  (`namespace_id`),
  UNIQUE KEY `schema_id_UNIQUE` (`namespace_id`),
  UNIQUE KEY `schema_uri_UNIQUE` (`namespace_uri`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `namespaces_instance_log`
--

DROP TABLE IF EXISTS `namespaces_instance_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `namespaces_instance_log` (
  `namespace_id` int(11) NOT NULL,
  `namespace_value_uri` varchar(200) NOT NULL,
  `log_type` varchar(45) default NULL,
  `crawl_id` int(11) NOT NULL,
  `dataset_id` int(11) default NULL,
  KEY `fk_schema_id_idx_schema_log` (`namespace_id`),
  KEY `fk_schema_instance_idx_schema_log` (`namespace_value_uri`),
  KEY `fk_crawl_idx_schema_log` (`crawl_id`),
  KEY `fk_dataset_idx_schema_log` (`dataset_id`),
  CONSTRAINT `fk_schema_schema_log` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_crawl_schema_log` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_dataset_schema_log` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`dataset_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `namespaces_instances`
--

DROP TABLE IF EXISTS `namespaces_instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `namespaces_instances` (
  `namespace_id` int(11) NOT NULL,
  `namespace_value_uri` varchar(200) NOT NULL,
  `namespace_value_type` binary(1) NOT NULL,
  `namespace_value_description` varchar(500) default NULL,
  PRIMARY KEY  (`namespace_value_uri`,`namespace_id`),
  KEY `fk_schema_id` (`namespace_id`),
  CONSTRAINT `fk_schema_id` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `namespaces_log`
--

DROP TABLE IF EXISTS `namespaces_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `namespaces_log` (
  `namespace_id` int(11) NOT NULL,
  `crawl_id` int(11) NOT NULL,
  `log_type` varchar(45) NOT NULL,
  KEY `fk_schema_idx` (`namespace_id`),
  KEY `fk_schema_crawl_idx` (`crawl_id`),
  CONSTRAINT `fk_schema_log_key_id` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_schema_log_crawl_log_id` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_instance_log`
--

DROP TABLE IF EXISTS `resource_instance_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_instance_log` (
  `resource_instance_log_id` int(11) NOT NULL auto_increment,
  `resource_id` int(11) NOT NULL default '0',
  `log_type` varchar(45) NOT NULL default '',
  `crawl_id` int(11) NOT NULL,
  UNIQUE KEY `resource_instance_log_id_UNIQUE` (`resource_instance_log_id`),
  KEY `resource_id_UNIQUE` (`resource_id`),
  KEY `crawl_id_UNIQUE` (`crawl_id`),
  CONSTRAINT `fk_crawl_resource_instance_log` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_resource_instance_resource_instance_log` FOREIGN KEY (`resource_id`) REFERENCES `resource_instances` (`resource_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=298860 DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 25600 kB; (`crawl_id`) REFER `ld_dataset_crawle';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_instance_type`
--

DROP TABLE IF EXISTS `resource_instance_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_instance_type` (
  `resource_id` int(11) NOT NULL,
  `type_id` int(11) NOT NULL,
  KEY `fk_resource_instance_id_resource_instance_type` (`resource_id`),
  KEY `fk_resource_type_id_resource_instance_type` (`type_id`),
  CONSTRAINT `fk_resource_instance_id_resource_instance_type` FOREIGN KEY (`resource_id`) REFERENCES `resource_instances` (`resource_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_resource_type_id_resource_instance_type` FOREIGN KEY (`type_id`) REFERENCES `resource_types` (`type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_instance_type_log`
--

DROP TABLE IF EXISTS `resource_instance_type_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_instance_type_log` (
  `resource_id` int(11) NOT NULL default '0',
  `type_id` int(11) NOT NULL,
  `log_type` varchar(45) NOT NULL default '',
  `crawl_id` int(11) NOT NULL,
  KEY `fk_resource_type_idx` (`type_id`),
  KEY `fk_resource_instance_idx` (`resource_id`),
  KEY `fk_crawl_idx` (`crawl_id`),
  KEY `log_type_idx` (`log_type`),
  CONSTRAINT `fk_crawl_resource_type_log` FOREIGN KEY (`crawl_id`) REFERENCES `dataset_crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_resource_instance_resource_type_log` FOREIGN KEY (`resource_id`) REFERENCES `resource_instances` (`resource_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_resource_type_resource_type_log` FOREIGN KEY (`type_id`) REFERENCES `resource_types` (`type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_instances`
--

DROP TABLE IF EXISTS `resource_instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_instances` (
  `resource_id` int(11) NOT NULL auto_increment,
  `resource_uri` varchar(500) NOT NULL default '',
  `dataset_id` int(11) default NULL,
  PRIMARY KEY  (`resource_id`),
  KEY `fk_dataset_idx` (`dataset_id`),
  KEY `res_uri_idx` (`resource_uri`(255)),
  CONSTRAINT `fk_dataset_resource_instances` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`dataset_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=284528 DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 29696 kB; (`dataset_id`) REFER `ld_dataset_craw';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_type_log`
--

DROP TABLE IF EXISTS `resource_type_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_type_log` (
  `type_id` int(11) NOT NULL default '0',
  `crawl_id` int(11) NOT NULL,
  `namespace_id` int(11) NOT NULL,
  `log_type` varchar(45) NOT NULL default '',
  KEY `fk_rtl_type_id` (`type_id`),
  KEY `fk_rtl_crawl_id` (`crawl_id`),
  KEY `fk_rtl_schema_id` (`namespace_id`),
  KEY `log_type_idx` (`log_type`),
  CONSTRAINT `fk_rtl_schema_id_key` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_rtl_crawl_id_key` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_rtl_type_id_key` FOREIGN KEY (`type_id`) REFERENCES `resource_types` (`type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_types`
--

DROP TABLE IF EXISTS `resource_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_types` (
  `type_id` int(11) NOT NULL auto_increment,
  `type_uri` varchar(200) NOT NULL default '',
  `type_description` varchar(500) default NULL,
  `namespace_id` int(11) NOT NULL,
  PRIMARY KEY  (`type_id`),
  UNIQUE KEY `type_idx` (`type_uri`),
  KEY `fk_schema_idx` (`namespace_id`),
  CONSTRAINT `fk_schema` FOREIGN KEY (`namespace_id`) REFERENCES `namespace` (`namespace_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=623 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_value_log`
--

DROP TABLE IF EXISTS `resource_value_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_value_log` (
  `resource_value_id` int(11) NOT NULL default '0',
  `log_type` varchar(45) NOT NULL default '',
  `crawl_id` int(11) NOT NULL,
  KEY `fk_resource_value_resource_value_log` (`resource_value_id`),
  KEY `fk_crawl_resource_value_log` (`crawl_id`),
  KEY `log_idx` (`log_type`),
  CONSTRAINT `fk_crawl_resource_value_log` FOREIGN KEY (`crawl_id`) REFERENCES `crawl_log` (`crawl_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_resource_value_resource_value_log` FOREIGN KEY (`resource_value_id`) REFERENCES `resource_values` (`resource_value_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 21504 kB; (`crawl_id`) REFER `ld_dataset_crawle';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_values`
--

DROP TABLE IF EXISTS `resource_values`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_values` (
  `resource_value_id` int(11) NOT NULL auto_increment,
  `resource_id` int(11) NOT NULL,
  `property_uri` varchar(500) NOT NULL default '',
  `value` longtext,
  PRIMARY KEY  (`resource_value_id`,`resource_id`),
  KEY `fk_resource_idx` (`resource_id`),
  KEY `resource_value_idx` (`resource_value_id`),
  KEY `prop_idx` (`property_uri`(255)),
  CONSTRAINT `fk_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource_instances` (`resource_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=5712621 DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 9216 kB; (`resource_id`) REFER `ld_dataset_craw';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `user_id` int(10) unsigned NOT NULL auto_increment,
  `user_name` varchar(45) NOT NULL default '',
  `user_pwd` varchar(45) NOT NULL default '',
  `is_valid` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-08-11 16:15:24
