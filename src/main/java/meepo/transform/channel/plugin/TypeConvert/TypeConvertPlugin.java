package meepo.transform.channel.plugin.TypeConvert;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import meepo.transform.channel.DataEvent;
import meepo.transform.channel.plugin.DefaultPlugin;
import meepo.transform.config.TaskContext;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Created by peiliping on 17-3-8.
 */
public class TypeConvertPlugin extends DefaultPlugin {

    private boolean needConvert = false;

    private List<NotMatchItem> notMatch = Lists.newArrayList();

    public TypeConvertPlugin(TaskContext context) {
        super(context);
    }

    @Override public void convert(DataEvent de) {
        if (this.needConvert) {
            this.notMatch.forEach(item -> item.convert(de));
        }
        super.convert(de);
    }

    @Override public void autoMatchSchema(List<Pair<String, Integer>> source, List<Pair<String, Integer>> sink) {
        if (sink.isEmpty()) {
            sink.addAll(source);
            this.needConvert = false;
            return;
        }
        _autoMatchSchema(source, sink);
    }

    public void _autoMatchSchema(List<Pair<String, Integer>> source, List<Pair<String, Integer>> sink) {
        Validate.isTrue(source.size() == sink.size());
        for (int i = 0; i < source.size(); i++) {
            Pair<String, Integer> l = source.get(i);
            Pair<String, Integer> r = sink.get(i);
            if (l.getRight().equals(r.getRight())) {
                continue;
            } else {
                this.notMatch.add(NotMatchItem.builder().columnIndex(i).sourceFieldName(l.getLeft()).sourceFieldType(l.getRight()).targetFieldName(r.getLeft())
                        .targetFieldType(r.getRight()).build().init());
            }
        }
        if (this.notMatch.size() > 0) {
            this.needConvert = true;
            LOG.info("Convert Result :", JSON.toJSONString(this.notMatch));
        }
    }
}
