package com.tencent.supersonic.chat.domain.pojo.search;

public enum QueryState {
    NORMAL(0),
    SEARCH_EXCEPTION(1),
    EMPTY(2),
    INVALID(3);

    private int state;

    QueryState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
