package com.tencent.supersonic.headless.chat.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

/** * word nature */
@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DictWord {

    private String word;
    private String nature;
    private String natureWithFrequency;
    private String alias;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DictWord that = (DictWord) o;
        return Objects.equals(word, that.word)
                && Objects.equals(natureWithFrequency, that.natureWithFrequency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, natureWithFrequency);
    }
}
