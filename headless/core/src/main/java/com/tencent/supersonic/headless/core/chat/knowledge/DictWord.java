package com.tencent.supersonic.headless.core.chat.knowledge;

import java.util.Objects;
import lombok.Data;
import lombok.ToString;

/***
 * word nature
 */
@Data
@ToString
public class DictWord {

    private String word;
    private String nature;
    private String natureWithFrequency;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DictWord that = (DictWord) o;
        return Objects.equals(word, that.word) && Objects.equals(natureWithFrequency, that.natureWithFrequency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, natureWithFrequency);
    }
}
