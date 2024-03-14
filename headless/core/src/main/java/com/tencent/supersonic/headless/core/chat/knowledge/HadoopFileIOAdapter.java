package com.tencent.supersonic.headless.core.chat.knowledge;

import com.hankcs.hanlp.corpus.io.IIOAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

@Slf4j
public class HadoopFileIOAdapter implements IIOAdapter {

    @Override
    public InputStream open(String path) throws IOException {
        log.info("open:{}", path);
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(path), conf);
        return fs.open(new Path(path));
    }

    @Override
    public OutputStream create(String path) throws IOException {
        log.info("create:{}", path);
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(path), conf);
        return fs.create(new Path(path));
    }
}
