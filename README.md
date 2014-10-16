#What?

Adopted the Camus Sweeper Project ([code](https://github.com/linkedin/camus/tree/master/camus-sweeper)) to tackle the 
Small File Issue together with Camus ([code](https://github.com/linkedin/camus))

Camus Sweeper aggregates the hourly [Avro](http://avro.apache.org/) files to daily Avro files

Camus Sweeper needs Avro-Schema files. Since I don't have a Schema Registry I added a configuration parameter for that:

```
# define the topic to schema class mappings here
# f.e. for topic eu_fakod_app_search_term you should configure:
eu_fakod_app_search_term.camus.sweeper.avro.schema.class=eu.fakod.app.search.term.SchemaClass
```