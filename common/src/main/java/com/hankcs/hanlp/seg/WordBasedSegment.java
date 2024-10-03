package com.hankcs.hanlp.seg;

import com.hankcs.hanlp.algorithm.Viterbi;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.collection.trie.DoubleArrayTrie;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CoreDictionaryTransformMatrixDictionary;
import com.hankcs.hanlp.dictionary.other.CharType;
import com.hankcs.hanlp.seg.NShort.Path.AtomNode;
import com.hankcs.hanlp.seg.common.Graph;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;
import com.hankcs.hanlp.utility.TextUtility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public abstract class WordBasedSegment extends Segment {

    public WordBasedSegment() {}

    protected static void generateWord(List<Vertex> linkedArray, WordNet wordNetOptimum) {
        fixResultByRule(linkedArray);
        wordNetOptimum.addAll(linkedArray);
    }

    protected static void fixResultByRule(List<Vertex> linkedArray) {
        mergeContinueNumIntoOne(linkedArray);
        changeDelimiterPOS(linkedArray);
        splitMiddleSlashFromDigitalWords(linkedArray);
        checkDateElements(linkedArray);
    }

    static void changeDelimiterPOS(List<Vertex> linkedArray) {
        Iterator var1 = linkedArray.iterator();

        while (true) {
            Vertex vertex;
            do {
                if (!var1.hasNext()) {
                    return;
                }

                vertex = (Vertex) var1.next();
            } while (!vertex.realWord.equals("－－") && !vertex.realWord.equals("—")
                    && !vertex.realWord.equals("-"));

            vertex.confirmNature(Nature.w);
        }
    }

    private static void splitMiddleSlashFromDigitalWords(List<Vertex> linkedArray) {
        if (linkedArray.size() >= 2) {
            ListIterator<Vertex> listIterator = linkedArray.listIterator();
            Vertex next = (Vertex) listIterator.next();

            for (Vertex current = next; listIterator.hasNext(); current = next) {
                next = (Vertex) listIterator.next();
                Nature currentNature = current.getNature();
                if (currentNature == Nature.nx
                        && (next.hasNature(Nature.q) || next.hasNature(Nature.n))) {
                    String[] param = current.realWord.split("-", 1);
                    if (param.length == 2 && TextUtility.isAllNum(param[0])
                            && TextUtility.isAllNum(param[1])) {
                        current = current.copy();
                        current.realWord = param[0];
                        current.confirmNature(Nature.m);
                        listIterator.previous();
                        listIterator.previous();
                        listIterator.set(current);
                        listIterator.next();
                        listIterator.add(Vertex.newPunctuationInstance("-"));
                        listIterator.add(Vertex.newNumberInstance(param[1]));
                    }
                }
            }
        }
    }

    private static void checkDateElements(List<Vertex> linkedArray) {
        if (linkedArray.size() >= 2) {
            ListIterator<Vertex> listIterator = linkedArray.listIterator();
            Vertex next = (Vertex) listIterator.next();

            for (Vertex current = next; listIterator.hasNext(); current = next) {
                next = (Vertex) listIterator.next();
                if (TextUtility.isAllNum(current.realWord)
                        || TextUtility.isAllChineseNum(current.realWord)) {
                    String nextWord = next.realWord;
                    if (nextWord.length() == 1 && "月日时分秒".contains(nextWord)
                            || nextWord.length() == 2 && nextWord.equals("月份")) {
                        mergeDate(listIterator, next, current);
                    } else if (nextWord.equals("年")) {
                        if (TextUtility.isYearTime(current.realWord)) {
                            mergeDate(listIterator, next, current);
                        } else {
                            current.confirmNature(Nature.m);
                        }
                    } else if (current.realWord.endsWith("点")) {
                        current.confirmNature(Nature.t, true);
                    } else {
                        char[] tmpCharArray = current.realWord.toCharArray();
                        String lastChar = String.valueOf(tmpCharArray[tmpCharArray.length - 1]);
                        if (!"∶·．／./".contains(lastChar)) {
                            current.confirmNature(Nature.m, true);
                        } else if (current.realWord.length() > 1) {
                            char last = current.realWord.charAt(current.realWord.length() - 1);
                            current = Vertex.newNumberInstance(
                                    current.realWord.substring(0, current.realWord.length() - 1));
                            listIterator.previous();
                            listIterator.previous();
                            listIterator.set(current);
                            listIterator.next();
                            listIterator.add(Vertex.newPunctuationInstance(String.valueOf(last)));
                        }
                    }
                }
            }
        }
    }

    private static void mergeDate(ListIterator<Vertex> listIterator, Vertex next, Vertex current) {
        current = Vertex.newTimeInstance(current.realWord + next.realWord);
        listIterator.previous();
        listIterator.previous();
        listIterator.set(current);
        listIterator.next();
        listIterator.next();
        listIterator.remove();
    }

    protected static List<Term> convert(List<Vertex> vertexList) {
        return Segment.convert(vertexList, false);
    }

    protected static Graph generateBiGraph(WordNet wordNet) {
        return wordNet.toGraph();
    }

    /** @deprecated */
    private static List<AtomNode> atomSegment(String sSentence, int start, int end) {
        if (end < start) {
            throw new RuntimeException("start=" + start + " < end=" + end);
        } else {
            List<AtomNode> atomSegment = new ArrayList();
            int pCur = 0;
            StringBuilder sb = new StringBuilder();
            char[] charArray = sSentence.substring(start, end).toCharArray();
            int[] charTypeArray = new int[charArray.length];

            for (int i = 0; i < charArray.length; ++i) {
                char c = charArray[i];
                charTypeArray[i] = CharType.get(c);
                if (c == '.' && i < charArray.length - 1 && CharType.get(charArray[i + 1]) == 9) {
                    charTypeArray[i] = 9;
                } else if (c == '.' && i < charArray.length - 1 && charArray[i + 1] >= '0'
                        && charArray[i + 1] <= '9') {
                    charTypeArray[i] = 5;
                } else if (charTypeArray[i] == 8) {
                    charTypeArray[i] = 5;
                }
            }

            while (true) {
                while (true) {
                    while (pCur < charArray.length) {
                        int nCurType = charTypeArray[pCur];
                        if (nCurType != 7 && nCurType != 10 && nCurType != 6 && nCurType != 17) {
                            if (pCur < charArray.length - 1 && (nCurType == 5 || nCurType == 9)) {
                                sb.delete(0, sb.length());
                                sb.append(charArray[pCur]);
                                boolean reachEnd = true;

                                while (pCur < charArray.length - 1) {
                                    ++pCur;
                                    int nNextType = charTypeArray[pCur];
                                    if (nNextType != nCurType) {
                                        reachEnd = false;
                                        break;
                                    }

                                    sb.append(charArray[pCur]);
                                }

                                atomSegment.add(new AtomNode(sb.toString(), nCurType));
                                if (reachEnd) {
                                    ++pCur;
                                }
                            } else {
                                atomSegment.add(new AtomNode(charArray[pCur], nCurType));
                                ++pCur;
                            }
                        } else {
                            String single = String.valueOf(charArray[pCur]);
                            if (single.length() != 0) {
                                atomSegment.add(new AtomNode(single, nCurType));
                            }

                            ++pCur;
                        }
                    }

                    return atomSegment;
                }
            }
        }
    }

    private static void mergeContinueNumIntoOne(List<Vertex> linkedArray) {
        if (linkedArray.size() >= 2) {
            ListIterator<Vertex> listIterator = linkedArray.listIterator();
            Vertex next = (Vertex) listIterator.next();
            Vertex current = next;

            while (true) {
                while (listIterator.hasNext()) {
                    next = (Vertex) listIterator.next();
                    if (!TextUtility.isAllNum(current.realWord)
                            && !TextUtility.isAllChineseNum(current.realWord)
                            || !TextUtility.isAllNum(next.realWord)
                                    && !TextUtility.isAllChineseNum(next.realWord)) {
                        current = next;
                    } else {
                        current = Vertex.newNumberInstance(current.realWord + next.realWord);
                        listIterator.previous();
                        listIterator.previous();
                        listIterator.set(current);
                        listIterator.next();
                        listIterator.next();
                        listIterator.remove();
                    }
                }

                return;
            }
        }
    }

    protected void generateWordNet(final WordNet wordNetStorage) {
        final char[] charArray = wordNetStorage.charArray;
        DoubleArrayTrie.Searcher searcher = CoreDictionary.trie.getSearcher(charArray, 0);

        while (searcher.next()) {
            wordNetStorage.add(searcher.begin + 1,
                    new Vertex(new String(charArray, searcher.begin, searcher.length),
                            (CoreDictionary.Attribute) searcher.value, searcher.index));
        }

        if (this.config.forceCustomDictionary) {
            this.customDictionary.parseText(charArray,
                    new AhoCorasickDoubleArrayTrie.IHit<CoreDictionary.Attribute>() {
                        public void hit(int begin, int end, CoreDictionary.Attribute value) {
                            wordNetStorage.add(begin + 1,
                                    new Vertex(new String(charArray, begin, end - begin), value));
                        }
                    });
        }

        LinkedList<Vertex>[] vertexes = wordNetStorage.getVertexes();
        int i = 1;

        while (true) {
            while (i < vertexes.length) {
                if (vertexes[i].isEmpty()) {
                    int j;
                    for (j = i + 1; j < vertexes.length - 1 && (vertexes[j].isEmpty()
                            || CharType.get(charArray[j - 1]) == 11); ++j) {
                    }

                    wordNetStorage.add(i, Segment.quickAtomSegment(charArray, i - 1, j - 1));
                    i = j;
                } else {
                    i += ((Vertex) vertexes[i].getLast()).realWord.length();
                }
            }

            return;
        }
    }

    protected List<Term> decorateResultForIndexMode(List<Vertex> vertexList, WordNet wordNetAll) {
        List<Term> termList = new LinkedList();
        int line = 1;
        ListIterator<Vertex> listIterator = vertexList.listIterator();
        listIterator.next();
        int length = vertexList.size() - 2;

        for (int i = 0; i < length; ++i) {
            Vertex vertex = (Vertex) listIterator.next();
            Term termMain = Segment.convert(vertex);
            // termList.add(termMain);
            addTerms(termList, vertex, line - 1);
            termMain.offset = line - 1;
            if (vertex.realWord.length() > 2) {
                label43: for (int currentLine = line; currentLine < line
                        + vertex.realWord.length(); ++currentLine) {
                    Iterator iterator = wordNetAll.descendingIterator(currentLine);

                    while (true) {
                        Vertex smallVertex;
                        do {
                            if (!iterator.hasNext()) {
                                continue label43;
                            }
                            smallVertex = (Vertex) iterator.next();
                        } while ((termMain.nature != Nature.mq || !smallVertex.hasNature(Nature.q))
                                && smallVertex.realWord.length() < this.config.indexMode);

                        if (smallVertex != vertex
                                && currentLine + smallVertex.realWord.length() <= line
                                        + vertex.realWord.length()) {
                            listIterator.add(smallVertex);
                            // Term termSub = convert(smallVertex);
                            // termSub.offset = currentLine - 1;
                            // termList.add(termSub);
                            addTerms(termList, smallVertex, currentLine - 1);
                        }
                    }
                }
            }

            line += vertex.realWord.length();
        }

        return termList;
    }

    protected static void speechTagging(List<Vertex> vertexList) {
        Viterbi.compute(vertexList,
                CoreDictionaryTransformMatrixDictionary.transformMatrixDictionary);
    }

    protected void addTerms(List<Term> terms, Vertex vertex, int offset) {
        for (int i = 0; i < vertex.attribute.nature.length; i++) {
            Term term = new Term(vertex.realWord, vertex.attribute.nature[i]);
            term.setFrequency(vertex.attribute.frequency[i]);
            term.offset = offset;
            terms.add(term);
        }
    }
}
