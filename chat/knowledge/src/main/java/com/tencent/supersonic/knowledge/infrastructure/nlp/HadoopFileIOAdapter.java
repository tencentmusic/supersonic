package com.tencent.supersonic.knowledge.infrastructure.nlp;

import com.hankcs.hanlp.corpus.io.IIOAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HadoopFileIOAdapter implements IIOAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopFileIOAdapter.class);

    @Override
    public InputStream open(String path) throws IOException {
        LOGGER.info("open:{}", path);
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(path), conf);
        return fs.open(new Path(path));
    }

    @Override
    public OutputStream create(String path) throws IOException {
        LOGGER.info("create:{}", path);
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(path), conf);
        return fs.create(new Path(path));
    }
}
