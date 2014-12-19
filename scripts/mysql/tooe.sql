-- MySQL dump 10.13  Distrib 5.1.61, for Win32 (ia32)
--
-- Host: localhost    Database: 
-- ------------------------------------------------------
-- Server version	5.1.61-community

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
-- Current Database: `mysql`
--

USE `mysql`;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;

delete from `user` where user='tooe';

-- INSERT INTO `user` VALUES ('localhost','tooe','*7233D2F88C152302A8847EC7AD627279DA6A5A0B','Y','Y','Y','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','','','','',0,0,0,0);
-- INSERT INTO `user` VALUES ('192.168.%.%','tooe','*7233D2F88C152302A8847EC7AD627279DA6A5A0B','Y','Y','Y','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','','','','',0,0,0,0);
-- INSERT INTO `user` VALUES ('10.0.%.%','tooe','*7233D2F88C152302A8847EC7AD627279DA6A5A0B','Y','Y','Y','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','N','','','','',0,0,0,0);

INSERT INTO `user` (Host,User,Password,Select_priv,Insert_priv,Update_priv) VALUES ('localhost','tooe','*7233D2F88C152302A8847EC7AD627279DA6A5A0B','Y','Y','Y');
INSERT INTO `user` (Host,User,Password,Select_priv,Insert_priv,Update_priv) VALUES ('192.168.%.%','tooe','*7233D2F88C152302A8847EC7AD627279DA6A5A0B','Y','Y','Y');
INSERT INTO `user` (Host,User,Password,Select_priv,Insert_priv,Update_priv) VALUES ('10.0.%.%','tooe','*7233D2F88C152302A8847EC7AD627279DA6A5A0B','Y','Y','Y');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tables_priv`
--

LOCK TABLES `tables_priv` WRITE;
/*!40000 ALTER TABLE `tables_priv` DISABLE KEYS */;

delete from `tables_priv` where db='tooe';

INSERT INTO `tables_priv` VALUES ('localhost','tooe','tooe','products','root@localhost','2013-01-24 15:58:00','Select,Insert,Update,Delete,Create,Drop,References,Index,Alter,Trigger',''),('localhost','tooe','tooe','paym_methods','root@localhost','2013-01-24 15:57:59','Select,Insert,Update,Delete,Create,Drop,References,Index,Alter,Trigger',''),('localhost','tooe','tooe','payments','root@localhost','2013-03-04 13:44:40','Select,Insert,Update,Delete,Create,Drop,References,Index,Alter,Trigger',''),('localhost','tooe','tooe','paym_systems','root@localhost','2013-03-04 13:44:40','Select,Insert,Update,Delete,Create,Drop,References,Index,Alter,Trigger',''),('localhost','tooe','tooe','recipients','root@localhost','2013-03-04 13:44:40','Select,Insert,Update,Delete,Create,Drop,References,Index,Alter,Trigger','');
/*!40000 ALTER TABLE `tables_priv` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Flush Grant Tables
--

/*! FLUSH PRIVILEGES */;

--
-- Current Database: `tooe`
--

/*!40000 DROP DATABASE IF EXISTS `tooe`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `tooe` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `tooe`;

--
-- Table structure for table `paym_systems`
--

DROP TABLE IF EXISTS `paym_systems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `paym_systems` (
  `order_id` BIGINT(21) UNSIGNED NOT NULL COMMENT 'Order number to pay',
  `paym_system` varchar(60) NOT NULL DEFAULT '' COMMENT 'Pay system',
  `sub_paym_system` varchar(60) DEFAULT '' COMMENT 'Pay subsystem',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Pay systems table through which orders was payed';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payments` (
  `order_id` BIGINT(21) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Order number to pay/Gift receiving code',
  `user_id` VARCHAR(24) COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'User ID payed for gift from MongoDB db.user._id',
  `trans_id` VARCHAR(100) COLLATE utf8_general_ci DEFAULT '' COMMENT 'Pay system transaction ID',
  `payment` DECIMAL(10,2) NOT NULL COMMENT 'Payment summ',
  `currency_id` VARCHAR(5) COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'Currency ID from MongoDB db.currency._id',
  `date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation timestamp',
  `expire_date` TIMESTAMP NULL COMMENT 'Pay possibility exhausting timestamp',
  `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'Order deletion flag',
  `rejected` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'Pay system rejection flag of this order',
  `received` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'Gift issue by location flag (with this order_id/gift receiving code)',
  `callback_uuid` VARCHAR(100) COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'UUID to compose callback URL',
  `exporttime` TIMESTAMP NULL COMMENT 'timestamp of export operation to 1C - last synchronization time',
  `exportcounter` int(2) NOT NULL DEFAULT '0' COMMENT 'couter of export operations to 1C',
  `exportupdatetime` TIMESTAMP NULL COMMENT 'timestamp of update confirmation in 1C - last update time',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `callback_uuid` (`callback_uuid`),
  UNIQUE KEY `trans_id` (`trans_id`),
  KEY `user_id` (`user_id`)
)ENGINE=InnoDB AUTO_INCREMENT=1000000 CHARACTER SET 'utf8' COLLATE 'utf8_general_ci'
COMMENT='Financial transactions table';/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `products` (
  `order_id` BIGINT(21) UNSIGNED NOT NULL COMMENT 'Order number to pay',
  `product_id` varchar(24) NOT NULL DEFAULT '' COMMENT 'Product ID from MongoDB db.product._id',
  `product_type` varchar(20) NOT NULL DEFAULT '' COMMENT 'Product type from MongoDB db.product_type._id',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT 'Product name from MongoDB db.product.n',
  `descr` varchar(200) NOT NULL DEFAULT '' COMMENT 'Product description from MongoDB db.product.d',
  `price` decimal(10,2) NOT NULL COMMENT 'Product price from MongoDB db.product.p.v',
  `currency` varchar(5) NOT NULL DEFAULT '' COMMENT 'Currency ID from MongoDB db.currency._id',
  `article` varchar(50) DEFAULT '' COMMENT 'Product article from MongoDB db.product.ar',
  `validity` int(3) unsigned NOT NULL COMMENT 'Validity term of the product from MongoDB db.product.v',
  `url` varchar(200) COMMENT 'Foto URL of the product from MongoDB db.product.pm.u',
  `company_id` varchar(24) NOT NULL DEFAULT '' COMMENT 'Company ID from MongoDB db.company._id',
  `loc_id` varchar(24) NOT NULL DEFAULT '' COMMENT 'Location ID from MongoDB db.location._id',
  `loc_name` varchar(100) NOT NULL DEFAULT '' COMMENT 'Location name from MongoDB db.location.n',
  `loc_city` varchar(50) NOT NULL DEFAULT '' COMMENT 'Location city from MongoDB db.location.lo.a.c',
  `loc_country` varchar(20) NOT NULL DEFAULT '' COMMENT 'Location country from MongoDB db.location.lo.a.co',
  `loc_street` varchar(200) NOT NULL DEFAULT '' COMMENT 'Location address from MongoDB db.location.lo.a.s',
  `present_msg` varchar(3000) DEFAULT '' COMMENT 'Message added on product payment',
  PRIMARY KEY (`order_id`),
  KEY `product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Payed gifts description table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recipients`
--

DROP TABLE IF EXISTS `recipients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `recipients` (
  `order_id` bigint(21) unsigned NOT NULL COMMENT 'Order number to pay',
  `recipient_id` varchar(24) DEFAULT '' COMMENT 'User ID being received gift',
  `email` varchar(50) DEFAULT '' COMMENT 'email of the user being received gift',
  `phone` varchar(20) DEFAULT '' COMMENT 'Phone of the user being received gift',
  `country_id` varchar(2) DEFAULT '' COMMENT 'Country code of the user being received gift',
  `country_pc` varchar(10) DEFAULT '' COMMENT 'Country phone code of the user being received gift',
  `show_actor` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Sender of the gift visibility flag (true default)',
  `isprivate` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'If news of this present should not be generated',
  PRIMARY KEY (`order_id`),
  KEY `recipient_id` (`recipient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Users table who received the gifts';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-03-04 16:22:35
