#### Linked Dataset Crawler
=================

The 'ld_crawler' tool (Linked Dataset Crawler) performs linked dataset crawling in an iterative manner. It stores the crawled datasets into a relational database, capturing all the changes in linked datasets. Furthermore, whenever there are  significant changes between two crawls of the same dataset, the corresponding changes and the most up-to-date state of the linked dataset is published using the Linked Dataset principles.

The tool itself captures changes in linked datasets at different levels. The first level is at the metadata level from datasets crawled from registries like the DataHub, such as the description of the dataset, endpoint url etc.

Another level is the association of a dataset with different namespaces, i.e. when in a specific dataset a namespace is introduced, that is, the namespace is extracted from the different datatype properties used to describe resource instances in a dataset, where we simply take the base URI of the datatype property.

Another linked dataset evolution capture is at the resource level. Resource instances in datasets get inserted, updated and deleted. An insert is captured easiest, by simply comparing two (or more) dataset crawls and whenever a new resource URI is detected it is considered as an insert. Similar is the case of deletions, in which case between two (or more) dataset crawls, whenever a resource URI is not present in the last crawl of the dataset then it is considered to be deleted. In the case of updates, we check whether a resource instance has a different number of triples (e.g. added types, deleted triples etc).

