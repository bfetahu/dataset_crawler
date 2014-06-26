### Linked Dataset Crawler

#### Introduction
=================


The 'ld_crawler' tool (Linked Dataset Crawler) performs linked dataset crawling in an iterative manner. It stores the crawled datasets into a relational database, capturing all the changes in linked datasets. Furthermore, whenever there are  significant changes between two crawls of the same dataset, the corresponding changes and the most up-to-date state of the linked dataset is published using the Linked Dataset principles.

The tool itself captures changes in linked datasets at different levels. The first level is at the metadata level from datasets crawled from registries like the DataHub, such as the description of the dataset, endpoint url etc.

Another level is the association of a dataset with different namespaces, i.e. when in a specific dataset a namespace is introduced, that is, the namespace is extracted from the different datatype properties used to describe resource instances in a dataset, where we simply take the base URI of the datatype property.

Another linked dataset evolution capture is at the resource level. Resource instances in datasets get inserted, updated and deleted. An insert is captured easiest, by simply comparing two (or more) dataset crawls and whenever a new resource URI is detected it is considered as an insert. Similar is the case of deletions, in which case between two (or more) dataset crawls, whenever a resource URI is not present in the last crawl of the dataset then it is considered to be deleted. In the case of updates, we check whether a resource instance has a different number of triples (e.g. added types, deleted triples etc).


#### Tool Configuration
=================

In order to perform the Linked Dataset crawls, there is a need to configure few parameters for the 'ld_crawler' tool. The configuration can be divided into two main steps: (1) database configuration and (2) tool parameter setup.

The database configuration is a simple step, where one needs to create a schema in a MySQL database by simply importing the schema dump ('dataset_crawler.sql'), and configuring the corresponding DB users for the schema.

Setting up the parameters for the 'ld_crawler' tool can be as following,  by providing the parameters and based on the example values (below) into a configuration file, which is given to the tool as an inline parameter, that is the path to the configuration file.

The URL for the MySQL host, e.g. localhost.
```
mysql_host="URL to the MySQL db host"
```

The name of the schema from the first step of DB configuration above.
```
mysql_schema=SCHEMA_NAME
```

The MySQL username that has read/write access to the schema.
```
mysql_user=USER_NAME
```

The MySQL password for the given username.
```
mysql_pwd=USER_PASSWORD
```

The path to the file containing the datasets to be crawled (see below).
```
dataset_to_crawl=PATH_TO_FILE
```

The timeout, which determines how long one waits for a SPARQL command to execute for a respective endpoint when crawling its data.
```
timeout=100000
```

The description of the dataset crawl.
```
crawl_description=Linked Dataset Crawl description.
```

##### Datasets to crawl file template

The file should contain per each line a dataset that needs to be crawled, where the different dataset attributes are separated with tab (\t) delimeter.

\# DATASET_ID  DATASET_ENDPOINT_URL  DATASET_DESCRIPTION

ted-talks http://data.linkededucation.org/request/ted/sparql  Metadata and transcripts of TED talks (www.ted.com).





