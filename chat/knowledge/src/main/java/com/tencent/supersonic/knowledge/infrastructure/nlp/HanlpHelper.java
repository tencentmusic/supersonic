package com.tencent.supersonic.knowledge.infrastructure.nlp;

import static com.hankcs.hanlp.HanLP.Config.CustomDictionaryPath;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import com.hankcs.hanlp.seg.Segment;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.WordNature;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

/**
 * HanLP helper
 */
public class HanlpHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HanlpHelper.class);
    public static final String FILE_SPILT = "/";
    public static final String SPACE_SPILT = "#";
    private static volatile Segment segment;
    public static volatile DynamicCustomDictionary CustomDictionary;
    public static final String DICT_MAIN_FILE_NAME = "CustomDictionary.txt";
    public static final String DICT_CLASS = "classes";

    static {
        // reset hanlp config
        try {
            resetHanlpConfig();
        } catch (FileNotFoundException e) {
            LOGGER.error("resetHanlpConfig error", e);
        }
    }

    public static Segment getSegment() {
        if (segment == null) {
            synchronized (HanlpHelper.class) {
                if (segment == null) {
                    segment = HanLP.newSegment()
                            .enableIndexMode(true).enableIndexMode(4)
                            .enableCustomDictionary(true).enableCustomDictionaryForcing(true).enableOffset(true)
                            .enableJapaneseNameRecognize(false).enableNameRecognize(false)
                            .enableAllNamedEntityRecognize(false)
                            .enableJapaneseNameRecognize(false).enableNumberQuantifierRecognize(false)
                            .enablePlaceRecognize(false)
                            .enableOrganizationRecognize(false).enableCustomDictionary(getDynamicCustomDictionary());
                }
            }
        }
        return segment;
    }

    public static DynamicCustomDictionary getDynamicCustomDictionary() {
        if (CustomDictionary == null) {
            synchronized (HanlpHelper.class) {
                if (CustomDictionary == null) {
                    CustomDictionary = new MultiCustomDictionary(CustomDictionaryPath);
                }
            }
        }
        return CustomDictionary;
    }

    /***
     * reload custom dictionary
     */
    public static boolean reloadCustomDictionary() throws IOException {

        LOGGER.info("reloadCustomDictionary start");

        final long startTime = System.currentTimeMillis();

        if (CustomDictionaryPath == null || CustomDictionaryPath.length == 0) {
            return false;
        }
        if (HanLP.Config.IOAdapter instanceof HadoopFileIOAdapter) {
            // 1.delete hdfs file
            HdfsFileHelper.deleteCacheFile(CustomDictionaryPath);
            // 2.query txt files，update CustomDictionaryPath
            HdfsFileHelper.resetCustomPath(getDynamicCustomDictionary());
        } else {
            FileHelper.deleteCacheFile(CustomDictionaryPath);
            FileHelper.resetCustomPath(getDynamicCustomDictionary());
        }
        // 3.clear trie
        Suggester.clear();

        boolean reload = getDynamicCustomDictionary().reload();
        LOGGER.info("reloadCustomDictionary end ,cost:{},reload:{}", System.currentTimeMillis() - startTime, reload);
        return reload;
    }

    private static void resetHanlpConfig() throws FileNotFoundException {
        if (HanLP.Config.IOAdapter instanceof HadoopFileIOAdapter) {
            return;
        }
        String hanlpPropertiesPath = getHanlpPropertiesPath();

        CustomDictionaryPath = Arrays.stream(CustomDictionaryPath).map(path -> hanlpPropertiesPath + FILE_SPILT + path)
                .toArray(String[]::new);
        LOGGER.info("hanlpPropertiesPath:{},CustomDictionaryPath:{}", hanlpPropertiesPath, CustomDictionaryPath);

        HanLP.Config.CoreDictionaryPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.BiGramDictionaryPath;
        HanLP.Config.CoreDictionaryTransformMatrixDictionaryPath = hanlpPropertiesPath + FILE_SPILT
                + HanLP.Config.CoreDictionaryTransformMatrixDictionaryPath;
        HanLP.Config.BiGramDictionaryPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.BiGramDictionaryPath;
        HanLP.Config.CoreStopWordDictionaryPath =
                hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CoreStopWordDictionaryPath;
        HanLP.Config.CoreSynonymDictionaryDictionaryPath = hanlpPropertiesPath + FILE_SPILT
                + HanLP.Config.CoreSynonymDictionaryDictionaryPath;
        HanLP.Config.PersonDictionaryPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PersonDictionaryPath;
        HanLP.Config.PersonDictionaryTrPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PersonDictionaryTrPath;

        HanLP.Config.PinyinDictionaryPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PinyinDictionaryPath;
        HanLP.Config.TranslatedPersonDictionaryPath = hanlpPropertiesPath + FILE_SPILT
                + HanLP.Config.TranslatedPersonDictionaryPath;
        HanLP.Config.JapanesePersonDictionaryPath = hanlpPropertiesPath + FILE_SPILT
                + HanLP.Config.JapanesePersonDictionaryPath;
        HanLP.Config.PlaceDictionaryPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PlaceDictionaryPath;
        HanLP.Config.PlaceDictionaryTrPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PlaceDictionaryTrPath;
        HanLP.Config.OrganizationDictionaryPath = hanlpPropertiesPath + FILE_SPILT
                + HanLP.Config.OrganizationDictionaryPath;
        HanLP.Config.OrganizationDictionaryTrPath = hanlpPropertiesPath + FILE_SPILT
                + HanLP.Config.OrganizationDictionaryTrPath;
        HanLP.Config.CharTypePath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CharTypePath;
        HanLP.Config.CharTablePath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CharTablePath;
        HanLP.Config.PartOfSpeechTagDictionary =
                hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PartOfSpeechTagDictionary;
        HanLP.Config.WordNatureModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.WordNatureModelPath;
        HanLP.Config.MaxEntModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.MaxEntModelPath;
        HanLP.Config.NNParserModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.NNParserModelPath;
        HanLP.Config.PerceptronParserModelPath =
                hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PerceptronParserModelPath;
        HanLP.Config.CRFSegmentModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CRFSegmentModelPath;
        HanLP.Config.HMMSegmentModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.HMMSegmentModelPath;
        HanLP.Config.CRFCWSModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CRFCWSModelPath;
        HanLP.Config.CRFPOSModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CRFPOSModelPath;
        HanLP.Config.CRFNERModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.CRFNERModelPath;
        HanLP.Config.PerceptronCWSModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PerceptronCWSModelPath;
        HanLP.Config.PerceptronPOSModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PerceptronPOSModelPath;
        HanLP.Config.PerceptronNERModelPath = hanlpPropertiesPath + FILE_SPILT + HanLP.Config.PerceptronNERModelPath;
    }

    public static String getHanlpPropertiesPath() throws FileNotFoundException {
        return ResourceUtils.getFile("classpath:hanlp.properties").getParent();
    }

    public static boolean addToCustomDictionary(WordNature wordNature) {
        LOGGER.debug("wordNature:{}", wordNature);
        return getDynamicCustomDictionary().insert(wordNature.getWord(), wordNature.getNatureWithFrequency());
    }

    public static void transLetterOriginal(List<MapResult> mapResults) {
        for (MapResult mapResult : mapResults) {
            if (MultiCustomDictionary.isLowerLetter(mapResult.getName())) {
                if (CustomDictionary.contains(mapResult.getName())) {
                    CoreDictionary.Attribute attribute = CustomDictionary.get(mapResult.getName());
                    if (attribute != null && attribute.original != null) {
                        mapResult.setName(attribute.original);
                    }
                }
            }
        }
    }


}
