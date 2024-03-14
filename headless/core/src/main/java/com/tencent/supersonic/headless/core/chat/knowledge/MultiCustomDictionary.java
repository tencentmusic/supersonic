package com.tencent.supersonic.headless.core.chat.knowledge;

import static com.hankcs.hanlp.utility.Predefine.logger;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.trie.DoubleArrayTrie;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.io.ByteArray;
import com.hankcs.hanlp.corpus.io.IOUtil;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import com.hankcs.hanlp.dictionary.other.CharTable;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.utility.LexiconUtility;
import com.hankcs.hanlp.utility.Predefine;
import com.hankcs.hanlp.utility.TextUtility;
import com.tencent.supersonic.headless.core.chat.knowledge.helper.HanlpHelper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class MultiCustomDictionary extends DynamicCustomDictionary {

    public static int MAX_SIZE = 10;
    public static Boolean removeDuplicates = true;
    public static ConcurrentHashMap<String, PriorityQueue<Term>> NATURE_TO_VALUES = new ConcurrentHashMap<>();
    private static boolean addToSuggesterTrie = true;

    public MultiCustomDictionary() {
        this(HanLP.Config.CustomDictionaryPath);
    }

    public MultiCustomDictionary(String... path) {
        super(path);
    }

    /***
     * load dictionary
     * @param path
     * @param defaultNature
     * @param map
     * @param customNatureCollector
     * @param addToSuggeterTrie
     * @return
     */
    public static boolean load(String path, Nature defaultNature, TreeMap<String, CoreDictionary.Attribute> map,
            LinkedHashSet<Nature> customNatureCollector, boolean addToSuggeterTrie) {
        try {
            String splitter = "\\s";
            if (path.endsWith(".csv")) {
                splitter = ",";
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(IOUtil.newInputStream(path), "UTF-8"));
            boolean firstLine = true;

            while (true) {
                String[] param;
                do {
                    String line;
                    if ((line = br.readLine()) == null) {
                        br.close();
                        return true;
                    }

                    if (firstLine) {
                        line = IOUtil.removeUTF8BOM(line);
                        firstLine = false;
                    }

                    param = line.split(splitter);
                } while (param[0].length() == 0);

                if (HanLP.Config.Normalization) {
                    param[0] = CharTable.convert(param[0]);
                }

                int natureCount = (param.length - 1) / 2;
                CoreDictionary.Attribute attribute;
                boolean isLetters = isLetters(param[0]);
                String original = null;
                String word = getWordBySpace(param[0]);
                if (isLetters) {
                    original = word;
                    word = word.toLowerCase();
                }
                if (natureCount == 0) {
                    attribute = new CoreDictionary.Attribute(defaultNature);
                } else {
                    attribute = new CoreDictionary.Attribute(natureCount);

                    for (int i = 0; i < natureCount; ++i) {
                        attribute.nature[i] = LexiconUtility.convertStringToNature(param[1 + 2 * i],
                                customNatureCollector);
                        attribute.frequency[i] = Integer.parseInt(param[2 + 2 * i]);
                        attribute.totalFrequency += attribute.frequency[i];
                    }
                }
                attribute.original = original;

                if (removeDuplicates && map.containsKey(word)) {
                    attribute = DictionaryAttributeUtil.getAttribute(map.get(word), attribute);
                }
                map.put(word, attribute);
                if (addToSuggeterTrie) {
                    SearchService.put(word, attribute);
                }
                for (int i = 0; i < attribute.nature.length; i++) {
                    Nature nature = attribute.nature[i];
                    PriorityQueue<Term> priorityQueue = NATURE_TO_VALUES.get(nature.toString());
                    if (Objects.isNull(priorityQueue)) {
                        priorityQueue = new PriorityQueue<>(MAX_SIZE,
                                Comparator.comparingInt(Term::getFrequency).reversed());
                        NATURE_TO_VALUES.put(nature.toString(), priorityQueue);
                    }
                    Term term = new Term(word, nature);
                    term.setFrequency(attribute.frequency[i]);
                    if (!priorityQueue.contains(term) && priorityQueue.size() < MAX_SIZE) {
                        priorityQueue.add(term);
                    }
                }
            }
        } catch (Exception var12) {
            logger.severe("自定义词典" + path + "读取错误！" + var12);
            return false;
        }
    }

    public boolean load(String... path) {
        this.path = path;
        long start = System.currentTimeMillis();
        if (!this.loadMainDictionary(path[0])) {
            Predefine.logger.warning("自定义词典" + Arrays.toString(path) + "加载失败");
            return false;
        } else {
            Predefine.logger.info(
                    "自定义词典加载成功:" + this.dat.size() + "个词条，耗时" + (System.currentTimeMillis() - start) + "ms");
            this.path = path;
            return true;
        }
    }

    /***
     * load main dictionary
     * @param mainPath
     * @param path
     * @param dat
     * @param isCache
     * @param addToSuggestTrie
     * @return
     */
    public static boolean loadMainDictionary(String mainPath, String[] path,
                                             DoubleArrayTrie<CoreDictionary.Attribute> dat, boolean isCache,
                                             boolean addToSuggestTrie) {
        Predefine.logger.info("自定义词典开始加载:" + mainPath);
        if (loadDat(mainPath, dat)) {
            return true;
        } else {
            TreeMap<String, CoreDictionary.Attribute> map = new TreeMap();
            LinkedHashSet customNatureCollector = new LinkedHashSet();

            try {
                for (String p : path) {
                    Nature defaultNature = Nature.n;
                    File file = new File(p);
                    String fileName = file.getName();
                    int cut = fileName.lastIndexOf(32);
                    if (cut > 0) {
                        String nature = fileName.substring(cut + 1);
                        p = file.getParent() + File.separator + fileName.substring(0, cut);

                        try {
                            defaultNature = LexiconUtility.convertStringToNature(nature, customNatureCollector);
                        } catch (Exception var16) {
                            Predefine.logger.severe("配置文件【" + p + "】写错了！" + var16);
                            continue;
                        }
                    }

                    Predefine.logger.info("以默认词性[" + defaultNature + "]加载自定义词典" + p + "中……");
                    boolean success = load(p, defaultNature, map, customNatureCollector, addToSuggestTrie);
                    if (!success) {
                        Predefine.logger.warning("失败：" + p);
                    }
                }

                if (map.size() == 0) {
                    Predefine.logger.warning("没有加载到任何词条");
                    map.put("未##它", null);
                }

                logger.info("正在构建DoubleArrayTrie……");
                dat.build(map);
                if (addToSuggestTrie) {
                    // SearchService.save();
                }
                if (isCache) {
                    // 缓存成dat文件，下次加载会快很多
                    logger.info("正在缓存词典为dat文件……");
                    // 缓存值文件
                    List<CoreDictionary.Attribute> attributeList = new LinkedList<CoreDictionary.Attribute>();
                    for (Map.Entry<String, CoreDictionary.Attribute> entry : map.entrySet()) {
                        attributeList.add(entry.getValue());
                    }

                    DataOutputStream out = new DataOutputStream(
                            new BufferedOutputStream(IOUtil.newOutputStream(mainPath + ".bin")));
                    if (customNatureCollector.isEmpty()) {
                        for (int i = Nature.begin.ordinal() + 1; i < Nature.values().length; ++i) {
                            Nature nature = Nature.values()[i];
                            if (Objects.nonNull(nature)) {
                                customNatureCollector.add(nature);
                            }
                        }
                    }

                    IOUtil.writeCustomNature(out, customNatureCollector);
                    out.writeInt(attributeList.size());

                    for (CoreDictionary.Attribute attribute : attributeList) {
                        attribute.save(out);
                    }

                    dat.save(out);
                    out.close();
                }
            } catch (FileNotFoundException var17) {
                logger.severe("自定义词典" + mainPath + "不存在！" + var17);
                return false;
            } catch (IOException var18) {
                logger.severe("自定义词典" + mainPath + "读取错误！" + var18);
                return false;
            } catch (Exception var19) {
                logger.warning("自定义词典" + mainPath + "缓存失败！\n" + TextUtility.exceptionToString(var19));
            }

            return true;
        }
    }

    public boolean loadMainDictionary(String mainPath) {
        return loadMainDictionary(mainPath, this.path, this.dat, true, addToSuggesterTrie);
    }

    public static boolean loadDat(String path, DoubleArrayTrie<CoreDictionary.Attribute> dat) {
        return loadDat(path, HanLP.Config.CustomDictionaryPath, dat);
    }

    public static boolean loadDat(String path, String[] customDicPath, DoubleArrayTrie<CoreDictionary.Attribute> dat) {
        try {
            if (HanLP.Config.CustomDictionaryAutoRefreshCache && isDicNeedUpdate(path, customDicPath)) {
                return false;
            } else {
                ByteArray byteArray = ByteArray.createByteArray(path + ".bin");
                if (byteArray == null) {
                    return false;
                } else {
                    int size = byteArray.nextInt();
                    if (size < 0) {
                        while (true) {
                            ++size;
                            if (size > 0) {
                                size = byteArray.nextInt();
                                break;
                            }

                            Nature.create(byteArray.nextString());
                        }
                    }

                    CoreDictionary.Attribute[] attributes = new CoreDictionary.Attribute[size];
                    Nature[] natureIndexArray = Nature.values();

                    for (int i = 0; i < size; ++i) {
                        int currentTotalFrequency = byteArray.nextInt();
                        int length = byteArray.nextInt();
                        attributes[i] = new CoreDictionary.Attribute(length);
                        attributes[i].totalFrequency = currentTotalFrequency;

                        for (int j = 0; j < length; ++j) {
                            attributes[i].nature[j] = natureIndexArray[byteArray.nextInt()];
                            attributes[i].frequency[j] = byteArray.nextInt();
                        }
                    }

                    if (!dat.load(byteArray, attributes)) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        } catch (Exception var11) {
            logger.warning("读取失败，问题发生在" + TextUtility.exceptionToString(var11));
            return false;
        }
    }

    public static boolean isLetters(String str) {
        char[] chars = str.toCharArray();
        if (chars.length <= 1) {
            return false;
        }
        for (int i = 0; i < chars.length; i++) {
            if ((chars[i] >= 'A' && chars[i] <= 'Z')) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLowerLetter(String str) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if ((chars[i] >= 'a' && chars[i] <= 'z')) {
                return true;
            }
        }
        return false;
    }

    public static String getWordBySpace(String word) {
        if (word.contains(HanlpHelper.SPACE_SPILT)) {
            return word.replace(HanlpHelper.SPACE_SPILT, " ");
        }
        return word;
    }

    public boolean reload() {
        if (this.path != null && this.path.length != 0) {
            IOUtil.deleteFile(this.path[0] + ".bin");
            Boolean loadCacheOk = this.loadDat(this.path[0], this.path, this.dat);
            if (!loadCacheOk) {
                return this.loadMainDictionary(this.path[0], this.path, this.dat, true, addToSuggesterTrie);
            }
        }
        return false;

    }

    public synchronized boolean insert(String word, String natureWithFrequency) {
        if (word == null) {
            return false;
        } else {
            if (HanLP.Config.Normalization) {
                word = CharTable.convert(word);
            }
            CoreDictionary.Attribute att = natureWithFrequency == null ? new CoreDictionary.Attribute(Nature.nz, 1)
                    : CoreDictionary.Attribute.create(natureWithFrequency);
            boolean isLetters = isLetters(word);
            word = getWordBySpace(word);
            String original = null;
            if (isLetters) {
                original = word;
                word = word.toLowerCase();
            }
            if (att == null) {
                return false;
            } else if (this.dat.containsKey(word)) {
                att.original = original;
                att = DictionaryAttributeUtil.getAttribute(this.dat.get(word), att);
                this.dat.set(word, att);
                // return true;
            } else {
                if (this.trie == null) {
                    this.trie = new BinTrie();
                }
                att.original = original;
                if (this.trie.containsKey(word)) {
                    att = DictionaryAttributeUtil.getAttribute(this.trie.get(word), att);
                }
                this.trie.put(word, att);
                // return true;
            }
            if (addToSuggesterTrie) {
                SearchService.put(word, att);
            }
            return true;
        }
    }
}
