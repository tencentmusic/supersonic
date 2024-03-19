package com.tencent.supersonic.common.pojo;

import com.google.common.base.Objects;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class RecordInfo {

    private String createdBy;

    private String updatedBy;

    private Date createdAt;

    private Date updatedAt;

    public RecordInfo createdBy(String userName) {
        this.createdBy = userName;
        this.createdAt = new Date();
        this.updatedBy = userName;
        this.updatedAt = new Date();
        return this;
    }

    public RecordInfo updatedBy(String userName) {
        this.updatedBy = userName;
        this.updatedAt = new Date();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordInfo that = (RecordInfo) o;
        return Objects.equal(createdBy, that.createdBy) && Objects.equal(
                updatedBy, that.updatedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(createdBy, updatedBy, createdAt, updatedAt);
    }
}
