package eu.fakod.mapreduce;

import com.linkedin.camus.sweeper.mapreduce.AvroKeyCombineFileInputFormat;
import com.linkedin.camus.sweeper.mapreduce.AvroKeyMapper;
import com.linkedin.camus.sweeper.mapreduce.AvroKeyReducer;
import com.linkedin.camus.sweeper.mapreduce.CamusSweeperJob;
import org.apache.avro.Schema;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroOutputFormat;
import org.apache.avro.mapred.AvroValue;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyOutputFormat;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;

import java.lang.reflect.Method;

public class CamusSweeperAvroJob extends CamusSweeperJob {
    @Override
    public void configureJob(String topic, Job job) {

        String avroTopicSchemaClass = getConfValue(job, topic, "camus.sweeper.avro.schema.class");
        if (avroTopicSchemaClass == null)
            return;

        // setting up our input format and map output types
        super.configureInput(job, AvroKeyCombineFileInputFormat.class, AvroKeyMapper.class, AvroKey.class, AvroValue.class);

        // setting up our output format and output types
        super.configureOutput(job, AvroKeyOutputFormat.class, AvroKeyReducer.class, AvroKey.class, NullWritable.class);

        Schema schema;

        try {
            Class<?> clazz = Class.forName(avroTopicSchemaClass);
            Method method = clazz.getMethod("getClassSchema");
            schema = (Schema) method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // checking if we have a key schema used for deduping. if we don't then we make this a map only
        // job and set the key schema
        // to the newest input schema
        String keySchemaStr = getConfValue(job, topic, "camus.sweeper.avro.key.schema");
        Schema keySchema;
        if (keySchemaStr == null || keySchemaStr.isEmpty()) {
            job.setNumReduceTasks(0);
            keySchema = schema;
        } else {
            keySchema = new Schema.Parser().parse(keySchemaStr);
        }

        setupSchemas(topic, job, schema, keySchema);

        // setting the compression level. Only used if compression is enabled. default is 6
        job.getConfiguration().setInt(AvroOutputFormat.DEFLATE_LEVEL_KEY,
                job.getConfiguration().getInt(AvroOutputFormat.DEFLATE_LEVEL_KEY, 6));
    }

    private void setupSchemas(String topic, Job job, Schema schema, Schema keySchema) {
        log.info("Input Schema set to " + schema.toString());
        AvroJob.setInputKeySchema(job, schema);

        AvroJob.setMapOutputKeySchema(job, keySchema);
        AvroJob.setMapOutputValueSchema(job, schema);

        Schema reducerSchema =
                new Schema.Parser().parse(getConfValue(job, topic, "camus.output.schema", schema.toString()));
        AvroJob.setOutputKeySchema(job, reducerSchema);
        log.info("Output Schema set to " + reducerSchema.toString());
    }

}

