package meepo.transform.source.parquet;

import com.google.common.collect.Lists;
import meepo.transform.channel.DataEvent;
import meepo.transform.channel.RingbufferChannel;
import meepo.transform.config.TaskContext;
import meepo.transform.report.SourceReportItem;
import meepo.transform.source.AbstractSource;
import meepo.util.parquet.ParquetTypeMapping;
import org.apache.commons.lang3.Validate;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.io.ParquetEncodingException;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Created by peiliping on 17-3-9.
 */
public class ParquetFileSource extends AbstractSource {

    private ParquetReader<Group>[] readers;

    private MessageType msgType;

    private List<PrimitiveType.PrimitiveTypeName> typeNames = Lists.newArrayList();
    private Group record;
    private int fileIndex;

    public ParquetFileSource(String name, int index, int totalNum, TaskContext context, RingbufferChannel rb) {
        super(name, index, totalNum, context, rb);
        Validate.isTrue(totalNum == 1);
        String inputDir = context.get("inputdir");
        Validate.notNull(inputDir);
        File dir = Paths.get(inputDir).toFile();
        Validate.isTrue(dir.isDirectory());
        List<String> fileNames = Lists.newArrayList(dir.list((dir1, name1) -> name1.endsWith(".parquet") || name1.endsWith(".pqt")));
        Validate.isTrue(fileNames.size() > 0);
        Collections.sort(fileNames);
        this.readers = new ParquetReader[fileNames.size()];

        try {
            Path filePath = new Path(inputDir + fileNames.get(0));
            this.msgType = (ParquetFileReader.readFooter(new Configuration(), filePath)).getFileMetaData().getSchema();
            this.msgType.getFields().forEach(type -> typeNames.add(type.asPrimitiveType().getPrimitiveTypeName()));
            super.columnsNum = this.msgType.getFieldCount();
            GroupReadSupport grs = new GroupReadSupport();
            grs.init(new Configuration(), null, this.msgType);
            super.getSchema().addAll(ParquetTypeMapping.convert2JDBCTypes(this.msgType));
            for (int i = 0; i < fileNames.size(); i++) {
                this.readers[i] = ParquetReader.builder(grs, new Path(inputDir + fileNames.get(i))).build();
            }
        } catch (Exception e) {
            LOG.error("Init Parquet Error :", e);
            Validate.isTrue(false);
        }
    }

    @Override
    public void work() throws Exception {
        if (this.fileIndex >= this.readers.length) {
            super.RUNNING = false;
            return;
        }
        this.record = this.readers[this.fileIndex].read();
        if (this.record == null) {
            this.readers[this.fileIndex].close();
            this.fileIndex++;
            return;
        }
        DataEvent de = super.feedOne();
        for (int i = 0; i < super.columnsNum; i++) {
            if (this.record.getFieldRepetitionCount(i) == 0) {
                de.getSource()[i] = null;
                continue;
            }
            switch (this.typeNames.get(i)) {
                case INT32:
                    de.getSource()[i] = this.record.getInteger(i, 0);
                    break;
                case INT64:
                    de.getSource()[i] = this.record.getLong(i, 0);
                    break;
                case BOOLEAN:
                    de.getSource()[i] = this.record.getBoolean(i, 0);
                    break;
                case BINARY:
                    de.getSource()[i] = this.record.getBinary(i, 0).toStringUsingUTF8();
                    break;
                case FLOAT:
                    de.getSource()[i] = this.record.getFloat(i, 0);
                    break;
                case DOUBLE:
                    de.getSource()[i] = this.record.getDouble(i, 0);
                    break;
                default:
                    throw new ParquetEncodingException("Unsupported column type: " + this.schema.get(i));
            }
        }
        super.pushOne();
    }

    @Override
    public SourceReportItem report() {
        return SourceReportItem.builder().name(this.taskName + "-Source-" + this.indexOfSources).start(0).current(this.fileIndex).end(this.readers.length).running(super.RUNNING)
                .build();
    }
}
