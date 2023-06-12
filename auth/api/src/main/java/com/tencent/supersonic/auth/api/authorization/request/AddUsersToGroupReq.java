package com.tencent.supersonic.auth.api.authorization.request;

import java.util.List;
import lombok.Data;

@Data
public class AddUsersToGroupReq {

    private Integer groupId;
    private List<String> users;
}
