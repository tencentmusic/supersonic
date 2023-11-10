package com.tencent.supersonic.semantic.api.query.pojo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DataDownload {

    List<List<String>> headers;

    List<List<String>> data;

}
