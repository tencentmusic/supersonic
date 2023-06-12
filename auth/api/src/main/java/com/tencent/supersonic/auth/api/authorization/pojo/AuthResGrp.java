package com.tencent.supersonic.auth.api.authorization.pojo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AuthResGrp {

    private List<AuthRes> group = new ArrayList<>();
}
