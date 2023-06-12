package com.tencent.supersonic.chat.domain.pojo.search;

import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@Getter
@ToString
public class MatchText {

    private String regText;

    private String detectSegment;

    public MatchText() {
    }


    public MatchText(String regText, String detectSegment) {
        this.regText = regText;
        this.detectSegment = detectSegment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatchText that = (MatchText) o;
        return Objects.equals(regText, that.regText) && Objects.equals(detectSegment, that.detectSegment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regText, detectSegment);
    }
}
