package com.tencent.supersonic.headless.api.pojo.response;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import lombok.Data;

@Data
public class S2Term {

    public String word;

    public Nature nature;
    public int offset;
    public int frequency = 0;

    public S2Term() {}

    public S2Term(String word, Nature nature) {
        this.word = word;
        this.nature = nature;
    }

    public S2Term(String word, Nature nature, int offset) {
        this.word = word;
        this.nature = nature;
        this.offset = offset;
    }

    public S2Term(String word, Nature nature, int offset, int frequency) {
        this.word = word;
        this.nature = nature;
        this.offset = offset;
        this.frequency = frequency;
    }

    public int length() {
        return this.word.length();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Term) {
            Term term = (Term) obj;
            if (this.nature == term.nature && this.word.equals(term.word)) {
                return true;
            }
        }
        return super.equals(obj);
    }
}
