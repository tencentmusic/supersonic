package com.tencent.supersonic.common.service;

import com.tencent.supersonic.common.pojo.SqlExemplar;

import java.util.List;

public interface ExemplarService {
    void storeExemplar(String collection, SqlExemplar exemplar);

    void removeExemplar(String collection, SqlExemplar exemplar);

    List<SqlExemplar> recallExemplars(String collection, String query, int num);

    List<SqlExemplar> recallExemplars(String query, int num);

}
