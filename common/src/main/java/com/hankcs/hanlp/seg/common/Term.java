package com.hankcs.hanlp.seg.common;

import com.hankcs.hanlp.corpus.tag.Nature;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Term {

    public String word;

    public Nature nature;
    public int offset;
    public int frequency = 0;

    public Term(String word, Nature nature) {
        this.word = word;
        this.nature = nature;
    }

    public int length() {
        return this.word.length();
    }

    public int getFrequency() {
        if (frequency > 0) {
            return frequency;
        }
        // todo opt
        /*
         * String wordOri = word.toLowerCase(); CoreDictionary.Attribute attribute =
         * getDynamicCustomDictionary().get(wordOri); if (attribute == null) { attribute =
         * CoreDictionary.get(wordOri); if (attribute == null) { attribute =
         * CustomDictionary.get(wordOri); } } if (attribute != null && nature != null &&
         * attribute.hasNature(nature)) { return attribute.getNatureFrequency(nature); } return
         * attribute == null ? 0 : attribute.totalFrequency;
         */
        return 0;
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
