package com.tencent.supersonic.chat.domain.pojo.semantic;

public enum DateTypeEnum {


    DAY("DAY", "天", "sys_imp_date"),
    WEEK("WEEK", "周", "sys_imp_week"),
    MONTH("MONTH", "月", "sys_imp_month"),
    YEAR("YEAR", "年", "sys_imp_year");
    private String code;

    private String name;

    private String field;


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getField() {
        return field;
    }

    DateTypeEnum(String code, String name, String field) {
        this.code = code;
        this.name = name;
        this.field = field;
    }

    public static DateTypeEnum fromCode(String code) {
        for (DateTypeEnum dateTypeEnum : DateTypeEnum.values()) {
            if (dateTypeEnum.getCode().equals(code)) {
                return dateTypeEnum;
            }
        }
        return DateTypeEnum.DAY;
    }


}