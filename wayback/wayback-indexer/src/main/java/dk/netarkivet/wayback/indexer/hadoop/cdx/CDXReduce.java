package dk.netarkivet.wayback.indexer.hadoop.cdx;

import com.google.common.collect.Lists;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CDXReduce extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

        List<String> valuesString = StreamSupport.stream(values.spliterator(), false).map(text -> text.toString()).collect(Collectors.toList());

        //Apparently this reverses the list
        valuesString = Lists.reverse(valuesString);

        context.write(null, new Text(String.join("\n",valuesString)));
    }
}
