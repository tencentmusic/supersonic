package com.tencent.supersonic.common.service;

import com.tencent.supersonic.common.pojo.Text2SQLExemplar;

import java.util.List;

public interface ExemplarService {
    void storeExemplar(String collection, Text2SQLExemplar exemplar);

    void removeExemplar(String collection, Text2SQLExemplar exemplar);

    List<Text2SQLExemplar> recallExemplars(String collection, String query, int num);

    List<Text2SQLExemplar> recallExemplars(String query, int num);

    void loadSysExemplars();
}
